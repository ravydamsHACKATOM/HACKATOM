package com.studyflow.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyflow.auth.AuthInterceptor;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** Sign-up, sign-in, sign-out and the isolation between accounts. */
@SpringBootTest(
    properties = "spring.datasource.url=jdbc:sqlite:target/studyflow-auth-test-${random.uuid}.db")
class AuthApiTest {

  @Autowired private WebApplicationContext context;
  @Autowired private ObjectMapper json;
  @Autowired private JdbcClient jdbc;

  private MockMvc mvc;

  @BeforeEach
  void cleanDatabase() {
    for (String table :
        new String[] {
          "disciplines", "availability_windows", "data_revision", "sessions", "users"
        }) {
      jdbc.sql("DELETE FROM " + table).update();
    }
    mvc = MockMvcBuilders.webAppContextSetup(context).build();
  }

  @Test
  void registeringSignsTheStudentIn() throws Exception {
    Cookie session =
        mvc.perform(postJson("/api/auth/register", account("Ana", "Ana@Example.com", "12345678")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Ana"))
            .andExpect(jsonPath("$.email").value("ana@example.com"))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.passwordHash").doesNotExist())
            .andExpect(cookie().httpOnly(AuthInterceptor.COOKIE, true))
            .andExpect(cookie().sameSite(AuthInterceptor.COOKIE, "Lax"))
            .andReturn()
            .getResponse()
            .getCookie(AuthInterceptor.COOKIE);

    mvc.perform(get("/api/auth/me").cookie(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("ana@example.com"));
  }

  @Test
  void storesOnlyAHashOfThePasswordAndOfTheSessionToken() throws Exception {
    Cookie session = TestAccounts.register(context, "ana@example.com");

    String passwordHash = jdbc.sql("SELECT password_hash FROM users").query(String.class).single();
    String tokenHash = jdbc.sql("SELECT token_hash FROM sessions").query(String.class).single();

    assertThat(passwordHash).startsWith("$2").doesNotContain(TestAccounts.PASSWORD);
    assertThat(tokenHash).isNotEqualTo(session.getValue()).hasSize(64);
  }

  @Test
  void rejectsASecondAccountWithTheSameEmail() throws Exception {
    TestAccounts.register(context, "ana@example.com");

    mvc.perform(postJson("/api/auth/register", account("Outra", "ANA@example.com", "12345678")))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));
  }

  @Test
  void rejectsWeakPasswordsAndInvalidEmails() throws Exception {
    mvc.perform(postJson("/api/auth/register", account("Ana", "not-an-email", "123")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.fields.email").exists())
        .andExpect(jsonPath("$.fields.password").exists());
  }

  @Test
  void logsInWithTheRightPassword() throws Exception {
    TestAccounts.register(context, "ana@example.com");

    mvc.perform(postJson("/api/auth/login", login("ana@example.com", TestAccounts.PASSWORD)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Ana"))
        .andExpect(cookie().exists(AuthInterceptor.COOKIE));
  }

  @Test
  void answersTheSameForAWrongPasswordAndAnUnknownEmail() throws Exception {
    TestAccounts.register(context, "ana@example.com");

    mvc.perform(postJson("/api/auth/login", login("ana@example.com", "wrong password")))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    mvc.perform(postJson("/api/auth/login", login("nobody@example.com", "wrong password")))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
  }

  @Test
  void blocksTheApiWithoutASession() throws Exception {
    mvc.perform(get("/api/disciplines"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    mvc.perform(get("/api/plan")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/disciplines").cookie(new Cookie(AuthInterceptor.COOKIE, "forged")))
        .andExpect(status().isUnauthorized());
    mvc.perform(get("/api/health")).andExpect(status().isOk());
  }

  @Test
  void loggingOutEndsTheSession() throws Exception {
    Cookie session = TestAccounts.register(context, "ana@example.com");

    mvc.perform(post("/api/auth/logout").cookie(session))
        .andExpect(status().isNoContent())
        .andExpect(cookie().maxAge(AuthInterceptor.COOKIE, 0));

    mvc.perform(get("/api/auth/me").cookie(session)).andExpect(status().isUnauthorized());
  }

  @Test
  void oneAccountNeverSeesOrChangesAnotherAccountsData() throws Exception {
    Cookie ana = TestAccounts.register(context, "ana@example.com");
    Cookie victor = TestAccounts.register(context, "victor@example.com");
    String discipline =
        """
        {"name": "Banco de Dados", "color": "#3F6B5C", "icon": "code", "weight": 2}
        """;
    long id =
        json.readTree(
                mvc.perform(postJson("/api/disciplines", discipline).cookie(ana))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString())
            .get("id")
            .asLong();

    mvc.perform(get("/api/disciplines").cookie(victor)).andExpect(jsonPath("$").isEmpty());
    mvc.perform(delete("/api/disciplines/" + id).cookie(victor)).andExpect(status().isNotFound());
    String event =
        """
        {"disciplineId": %d, "title": "Prova", "type": "EXAM",
         "dueDate": "2026-12-10", "effortHours": 2}
        """
            .formatted(id);
    mvc.perform(postJson("/api/events", event).cookie(victor))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("DISCIPLINE_NOT_FOUND"));

    mvc.perform(get("/api/disciplines").cookie(ana))
        .andExpect(jsonPath("$[0].name").value("Banco de Dados"));
  }

  private static String account(String name, String email, String password) {
    return """
        {"name": "%s", "email": "%s", "password": "%s"}
        """
        .formatted(name, email, password);
  }

  private static String login(String email, String password) {
    return """
        {"email": "%s", "password": "%s"}
        """
        .formatted(email, password);
  }

  private static MockHttpServletRequestBuilder postJson(String url, String body) {
    return post(url).contentType(MediaType.APPLICATION_JSON).content(body);
  }
}
