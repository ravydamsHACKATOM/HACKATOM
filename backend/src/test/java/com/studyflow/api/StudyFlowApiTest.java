package com.studyflow.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Drives the real application through HTTP against a throwaway SQLite file, with the clock frozen
 * on Monday 2026-11-23 at 08:00 so the generated schedule is predictable.
 */
@SpringBootTest(
    properties = {"spring.datasource.url=jdbc:sqlite:target/studyflow-test-${random.uuid}.db"})
class StudyFlowApiTest {

  private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

  @TestConfiguration
  static class FixedClock {

    @Bean
    @Primary
    Clock testClock() {
      return Clock.fixed(LocalDateTime.of(2026, 11, 23, 8, 0).atZone(ZONE).toInstant(), ZONE);
    }
  }

  @Autowired private WebApplicationContext context;
  @Autowired private ObjectMapper json;
  @Autowired private JdbcClient jdbc;

  /** Sends the session cookie of a freshly registered student on every request. */
  private MockMvc mvc;

  @BeforeEach
  void signInAsNewStudent() throws Exception {
    for (String table :
        new String[] {
          "study_blocks",
          "study_events",
          "availability_windows",
          "disciplines",
          "plan_notes",
          "data_revision",
          "sessions",
          "users"
        }) {
      jdbc.sql("DELETE FROM " + table).update();
    }
    Cookie session = TestAccounts.register(context, "ana@example.com");
    mvc =
        MockMvcBuilders.webAppContextSetup(context)
            .defaultRequest(get("/").cookie(session))
            .build();
  }

  @Test
  void healthEndpointAnswers() throws Exception {
    mvc.perform(get("/api/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ok"));
  }

  @Test
  void generatesAWeekFromEventsAndAvailability() throws Exception {
    long discipline = createDiscipline("Banco de Dados");
    createEvent(discipline, "Prova de SQL", "EXAM", "2026-11-26", 2.5);
    createWindow("MONDAY", "18:00", "20:00");
    createWindow("TUESDAY", "18:00", "20:00");

    JsonNode plan = generate();

    assertThat(plan.get("generated").asBoolean()).isTrue();
    assertThat(plan.get("stale").asBoolean()).isFalse();
    assertThat(plan.get("blocks")).hasSize(3);
    assertThat(plan.at("/blocks/0/date").asText()).isEqualTo("2026-11-23");
    assertThat(plan.at("/blocks/0/startTime").asText()).isEqualTo("18:00:00");
    assertThat(plan.at("/blocks/0/disciplineName").asText()).isEqualTo("Banco de Dados");
    assertThat(plan.at("/blocks/2/kind").asText()).isEqualTo("REVIEW");
    assertThat(plan.at("/stats/plannedMinutes").asInt()).isEqualTo(150);
    assertThat(plan.get("warnings")).isEmpty();
    assertThat(plan.at("/note/summary").asText()).contains("2h30");
  }

  @Test
  void warnsWhenTheWeekCannotFitTheEffort() throws Exception {
    long discipline = createDiscipline("Redes");
    createEvent(discipline, "Trabalho de Redes", "ASSIGNMENT", "2026-11-24", 5);
    createWindow("MONDAY", "18:00", "20:00");

    JsonNode plan = generate();

    assertThat(plan.at("/stats/plannedMinutes").asInt()).isEqualTo(100);
    assertThat(plan.at("/warnings/0/title").asText()).isEqualTo("Trabalho de Redes");
    assertThat(plan.at("/warnings/0/missingMinutes").asInt()).isEqualTo(200);
    assertThat(plan.at("/note/summary").asText()).contains("faltam 3h20");
  }

  @Test
  void keepsDoneBlocksAndReplansTheRest() throws Exception {
    long discipline = createDiscipline("Programação");
    createEvent(discipline, "Entrega da API", "ASSIGNMENT", "2026-11-27", 3);
    createWindow("MONDAY", "18:00", "20:00");
    createWindow("WEDNESDAY", "18:00", "20:00");
    JsonNode first = generate();
    long doneBlock = first.at("/blocks/0/id").asLong();

    mvc.perform(patchJson("/api/blocks/" + doneBlock + "/done", "{\"value\":true}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("DONE"));
    JsonNode second = generate();

    assertThat(second.at("/blocks/0/id").asLong()).isEqualTo(doneBlock);
    assertThat(second.at("/blocks/0/status").asText()).isEqualTo("DONE");
    assertThat(second.at("/stats/doneMinutes").asInt()).isEqualTo(50);
    assertThat(second.at("/stats/plannedMinutes").asInt()).isEqualTo(130);
    assertThat(second.at("/changes/added").asInt()).isZero();
  }

  @Test
  void postponingABlockMovesTheEffortElsewhere() throws Exception {
    long discipline = createDiscipline("Programação");
    createEvent(discipline, "Entrega da API", "ASSIGNMENT", "2026-11-27", 1.5);
    createWindow("MONDAY", "18:00", "20:00");
    createWindow("WEDNESDAY", "18:00", "20:00");
    JsonNode first = generate();
    long postponed = first.at("/blocks/0/id").asLong();

    JsonNode after =
        read(
            mvc.perform(post("/api/blocks/" + postponed + "/postpone")).andExpect(status().isOk()));

    assertThat(after.at("/blocks/0/status").asText()).isEqualTo("SKIPPED");
    assertThat(after.at("/stats/plannedMinutes").asInt()).isEqualTo(90);
    assertThat(after.at("/changes/added").asInt()).isPositive();
    assertThat(after.at("/note/summary").asText()).startsWith("Refiz o plano");
    assertThat(after.findValuesAsText("date")).contains("2026-11-25");
  }

  @Test
  void lockedBlocksSurviveARegeneration() throws Exception {
    long discipline = createDiscipline("Programação");
    long event = createEvent(discipline, "Entrega da API", "ASSIGNMENT", "2026-11-27", 1);
    createWindow("WEDNESDAY", "18:00", "20:00");
    JsonNode first = generate();
    long locked = first.at("/blocks/0/id").asLong();
    mvc.perform(patchJson("/api/blocks/" + locked + "/locked", "{\"value\":true}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.locked").value(true));

    createWindow("MONDAY", "18:00", "20:00");
    updateEvent(event, discipline, "Entrega da API", "ASSIGNMENT", "2026-11-27", 2);
    JsonNode second = generate();

    assertThat(second.findValuesAsText("id")).contains(String.valueOf(locked));
    assertThat(second.at("/stats/plannedMinutes").asInt()).isEqualTo(120);
  }

  @Test
  void marksThePlanAsStaleWhenADeadlineChanges() throws Exception {
    long discipline = createDiscipline("Redes");
    long event = createEvent(discipline, "Trabalho de Redes", "ASSIGNMENT", "2026-11-27", 1);
    createWindow("MONDAY", "18:00", "20:00");
    generate();

    updateEvent(event, discipline, "Trabalho de Redes", "ASSIGNMENT", "2026-11-25", 1);

    mvc.perform(get("/api/plan"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stale").value(true));
  }

  @Test
  void anUngeneratedWeekHasNoBlocksAndNoWarnings() throws Exception {
    long discipline = createDiscipline("Redes");
    createEvent(discipline, "Trabalho de Redes", "ASSIGNMENT", "2026-11-25", 4);

    mvc.perform(get("/api/plan"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.generated").value(false))
        .andExpect(jsonPath("$.blocks").isEmpty())
        .andExpect(jsonPath("$.warnings").isEmpty());
  }

  @Test
  void rejectsAWeekThatAlreadyEnded() throws Exception {
    mvc.perform(post("/api/plan/generate").param("date", "2026-11-10"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("WEEK_IN_PAST"));
  }

  @Test
  void rejectsInvalidEvents() throws Exception {
    long discipline = createDiscipline("Redes");

    mvc.perform(
            postJson(
                "/api/events",
                """
                {"disciplineId": %d, "title": " ", "type": "EXAM",
                 "dueDate": "2026-11-26", "effortHours": 0}
                """
                    .formatted(discipline)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.fields.title").exists())
        .andExpect(jsonPath("$.fields.effortHours").exists());
  }

  @Test
  void rejectsEventsOfUnknownDisciplines() throws Exception {
    mvc.perform(
            postJson(
                "/api/events",
                """
                {"disciplineId": 999, "title": "Prova", "type": "EXAM",
                 "dueDate": "2026-11-26", "effortHours": 2}
                """))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("DISCIPLINE_NOT_FOUND"));
  }

  @Test
  void rejectsOverlappingAvailability() throws Exception {
    createWindow("MONDAY", "18:00", "20:00");

    mvc.perform(postJson("/api/availability", windowJson("MONDAY", "19:00", "21:00")))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("AVAILABILITY_OVERLAP"));
  }

  @Test
  void rejectsWindowsShorterThanOneSession() throws Exception {
    mvc.perform(postJson("/api/availability", windowJson("MONDAY", "18:00", "18:10")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
  }

  @Test
  void deletingADisciplineRemovesItsEventsAndBlocks() throws Exception {
    long discipline = createDiscipline("Redes");
    createEvent(discipline, "Trabalho de Redes", "ASSIGNMENT", "2026-11-27", 1);
    createWindow("MONDAY", "18:00", "20:00");
    generate();

    mvc.perform(delete("/api/disciplines/" + discipline)).andExpect(status().isNoContent());

    mvc.perform(get("/api/events")).andExpect(jsonPath("$").isEmpty());
    mvc.perform(get("/api/plan")).andExpect(jsonPath("$.blocks").isEmpty());
  }

  @Test
  void reportsEventProgress() throws Exception {
    long discipline = createDiscipline("Redes");
    createEvent(discipline, "Trabalho de Redes", "ASSIGNMENT", "2026-11-27", 1.5);
    createWindow("MONDAY", "18:00", "20:00");
    JsonNode plan = generate();
    mvc.perform(
        patchJson("/api/blocks/" + plan.at("/blocks/0/id").asLong() + "/done", "{\"value\":true}"));

    mvc.perform(get("/api/events"))
        .andExpect(jsonPath("$[0].effortMinutes").value(90))
        .andExpect(jsonPath("$[0].doneMinutes").value(50))
        .andExpect(jsonPath("$[0].plannedMinutes").value(40));
  }

  @Test
  void unknownResourcesReturnNotFound() throws Exception {
    mvc.perform(delete("/api/events/12345"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }

  private JsonNode generate() throws Exception {
    return read(mvc.perform(post("/api/plan/generate")).andExpect(status().isOk()));
  }

  private long createDiscipline(String name) throws Exception {
    String body =
        """
        {"name": "%s", "color": "#3F6B5C", "icon": "book", "weight": 2}
        """
            .formatted(name);
    return read(mvc.perform(postJson("/api/disciplines", body)).andExpect(status().isCreated()))
        .get("id")
        .asLong();
  }

  private long createEvent(long discipline, String title, String type, String dueDate, double hours)
      throws Exception {
    return read(mvc.perform(
                postJson("/api/events", eventJson(discipline, title, type, dueDate, hours)))
            .andExpect(status().isCreated()))
        .get("id")
        .asLong();
  }

  private void updateEvent(
      long id, long discipline, String title, String type, String dueDate, double hours)
      throws Exception {
    mvc.perform(
            put("/api/events/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(eventJson(discipline, title, type, dueDate, hours)))
        .andExpect(status().isOk());
  }

  private void createWindow(String day, String start, String end) throws Exception {
    mvc.perform(postJson("/api/availability", windowJson(day, start, end)))
        .andExpect(status().isCreated());
  }

  private static String eventJson(
      long discipline, String title, String type, String dueDate, double hours) {
    return """
        {"disciplineId": %d, "title": "%s", "type": "%s", "dueDate": "%s", "effortHours": %s}
        """
        .formatted(discipline, title, type, dueDate, hours);
  }

  private static String windowJson(String day, String start, String end) {
    return """
        {"dayOfWeek": "%s", "startTime": "%s", "endTime": "%s"}
        """
        .formatted(day, start, end);
  }

  private static MockHttpServletRequestBuilder postJson(String url, String body) {
    return post(url).contentType(MediaType.APPLICATION_JSON).content(body);
  }

  private static MockHttpServletRequestBuilder patchJson(String url, String body) {
    return patch(url).contentType(MediaType.APPLICATION_JSON).content(body);
  }

  private JsonNode read(ResultActions result) throws Exception {
    return json.readTree(result.andReturn().getResponse().getContentAsString());
  }
}
