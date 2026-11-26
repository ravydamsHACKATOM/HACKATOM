package com.example.studyFlow;

import com.example.studyFlow.repository.AvailabilityRepository;
import com.example.studyFlow.repository.StudyBlockRepository;
import com.example.studyFlow.repository.StudyEventRepository;
import com.example.studyFlow.repository.SubjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class StudyFlowApiIntegrationTests {
    private static final String TEST_DATABASE = Path.of(System.getProperty("java.io.tmpdir"), "studyflow-" + UUID.randomUUID() + ".db").toString();

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> "jdbc:sqlite:" + TEST_DATABASE);
        properties.add("STUDYFLOW_DB_PATH", () -> TEST_DATABASE);
        properties.add("studyflow.cors.allowed-origins", () -> "http://localhost:8080");
    }

    @Autowired MockMvc mvc;
    @Autowired SubjectRepository subjects;
    @Autowired StudyEventRepository events;
    @Autowired StudyBlockRepository blocks;
    @Autowired AvailabilityRepository availability;

    @BeforeEach
    void clearDatabase() {
        blocks.deleteAll();
        events.deleteAll();
        subjects.deleteAll();
        availability.deleteAll();
    }

    @Test
    void startsTheWholeApplicationContext() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void serverRenderedFallbackWorksWithoutJavaScript() throws Exception {
        mvc.perform(get("/no-js")).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("MODO SEM JAVASCRIPT")));
        mvc.perform(post("/no-js/subjects").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("name", "Fallback subject").param("color", "#2858c8"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/api/subjects")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Fallback subject"));
    }

    @Test
    void validatesAndPerformsSubjectAndEventCrud() throws Exception {
        mvc.perform(post("/api/subjects").contentType("application/json").content("{\"name\":\"  \"}"))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        long subjectId = createSubject("Matemática");
        mvc.perform(put("/api/subjects/{id}", subjectId).contentType("application/json").content("{\"name\":\"Cálculo\",\"color\":\"#2858c8\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Cálculo"));

        mvc.perform(post("/api/events").contentType("application/json")
                        .content("{\"title\":\"Inválido\",\"type\":\"OUTRO\",\"subjectId\":" + subjectId
                                + ",\"dueDate\":\"" + LocalDate.now().plusDays(5) + "\",\"estimatedHours\":1}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        mvc.perform(post("/api/events").contentType("application/json")
                        .content("{\"title\":\"Inválido\",\"type\":\"PROVA\",\"subjectId\":" + subjectId
                                + ",\"dueDate\":\"" + LocalDate.now().minusDays(1) + "\",\"estimatedHours\":1}"))
                .andExpect(status().isBadRequest());

        long eventId = createEvent(subjectId, 2.0);
        mvc.perform(get("/api/events")).andExpect(status().isOk()).andExpect(jsonPath("$[0].dueDate").exists())
                .andExpect(jsonPath("$[0].estimatedHours").value(2.0));
        mvc.perform(put("/api/events/{id}", eventId).contentType("application/json").content(eventBody(subjectId, 1.5)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estimatedHours").value(1.5));
        mvc.perform(delete("/api/events/{id}", eventId)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/subjects/{id}", subjectId)).andExpect(status().isNoContent());
        assertThat(events.count()).isZero();
        assertThat(subjects.count()).isZero();
    }

    @Test
    void replacesAndValidatesWeeklyAvailability() throws Exception {
        mvc.perform(put("/api/availability").contentType("application/json")
                        .content("{\"daysOfWeek\":[1,3,7],\"startTime\":\"09:00\",\"endTime\":\"12:00\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].daysOfWeek", contains(1, 3, 7)));
        mvc.perform(put("/api/availability").contentType("application/json")
                        .content("{\"daysOfWeek\":[0],\"startTime\":\"09:00\",\"endTime\":\"08:00\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        mvc.perform(delete("/api/availability")).andExpect(status().isNoContent());
        mvc.perform(get("/api/availability")).andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void generatesNonOverlappingBlocksBeforeTheDeadlineAndPreservesCompletion() throws Exception {
        long subjectId = createSubject("História");
        long eventId = createEvent(subjectId, 2.0);
        setEveryDayAvailability();

        MvcResult firstResult = mvc.perform(post("/api/schedule/generate"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.warnings").isEmpty())
                .andExpect(jsonPath("$.blocks", hasSize(3))).andReturn();
        String firstJson = firstResult.getResponse().getContentAsString();
        long completedId = ((Number) com.jayway.jsonpath.JsonPath.read(firstJson, "$.blocks[0].id")).longValue();
        mvc.perform(patch("/api/blocks/{id}/completion", completedId).contentType("application/json").content("{\"completed\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.completed").value(true));
        mvc.perform(patch("/api/blocks/{id}/completion", completedId).contentType("application/json").content("{\"completed\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.completed").value(false));
        mvc.perform(patch("/api/blocks/{id}/completion", completedId).contentType("application/json").content("{\"completed\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.completed").value(true));

        mvc.perform(post("/api/schedule/generate"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.blocks", hasSize(3)))
                .andExpect(jsonPath("$.blocks[?(@.id == " + completedId + ")].completed", contains(true)));

        var persisted = blocks.findAllByOrderByStartAtAsc();
        assertThat(persisted).hasSize(3);
        assertThat(persisted).allMatch(block -> block.getStartAt().toLocalDate().isBefore(events.findById(eventId).orElseThrow().getDueDate()));
        assertThat(persisted).allMatch(block -> block.getEvent().getId().equals(eventId));
        for (int i = 0; i < persisted.size(); i++) {
            long minutes = java.time.Duration.between(persisted.get(i).getStartAt(), persisted.get(i).getEndAt()).toMinutes();
            assertThat(minutes).isBetween(1L, 50L);
            if (i > 0) assertThat(persisted.get(i).getStartAt()).isAfterOrEqualTo(persisted.get(i - 1).getEndAt());
        }
    }

    @Test
    void deletesSubjectAndEventDependenciesAndReturnsConsistentNotFoundErrors() throws Exception {
        long subjectId = createSubject("Química");
        long eventId = createEvent(subjectId, 1.0);
        setEveryDayAvailability();
        mvc.perform(post("/api/schedule/generate")).andExpect(status().isOk());
        assertThat(blocks.count()).isPositive();
        mvc.perform(delete("/api/subjects/{id}", subjectId)).andExpect(status().isNoContent());
        assertThat(events.count()).isZero();
        assertThat(blocks.count()).isZero();
        mvc.perform(delete("/api/events/{id}", eventId)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        mvc.perform(patch("/api/blocks/{id}/completion", 999999).contentType("application/json").content("{\"completed\":true}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void deletingAnEventAlsoDeletesItsStudyBlocks() throws Exception {
        long subjectId = createSubject("Biologia");
        long eventId = createEvent(subjectId, 1.0);
        setEveryDayAvailability();
        mvc.perform(post("/api/schedule/generate")).andExpect(status().isOk());
        assertThat(blocks.findByEventId(eventId)).isNotEmpty();
        mvc.perform(delete("/api/events/{id}", eventId)).andExpect(status().isNoContent());
        assertThat(blocks.findByEventId(eventId)).isEmpty();
        assertThat(subjects.existsById(subjectId)).isTrue();
    }

    private long createSubject(String name) throws Exception {
        MvcResult result = mvc.perform(post("/api/subjects").contentType("application/json")
                        .content("{\"name\":\"" + name + "\",\"color\":\"#2858c8\"}"))
                .andExpect(status().isCreated()).andReturn();
        return ((Number) com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private long createEvent(long subjectId, double effort) throws Exception {
        MvcResult result = mvc.perform(post("/api/events").contentType("application/json").content(eventBody(subjectId, effort)))
                .andExpect(status().isCreated()).andReturn();
        return ((Number) com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private String eventBody(long subjectId, double effort) {
        return "{\"title\":\"Prova de unidade\",\"type\":\"PROVA\",\"subjectId\":" + subjectId
                + ",\"dueDate\":\"" + LocalDate.now().plusDays(10) + "\",\"estimatedHours\":" + effort + "}";
    }

    private void setEveryDayAvailability() throws Exception {
        mvc.perform(put("/api/availability").contentType("application/json")
                        .content("{\"daysOfWeek\":[1,2,3,4,5,6,7],\"startTime\":\"08:00\",\"endTime\":\"12:00\"}"))
                .andExpect(status().isOk());
    }
}
