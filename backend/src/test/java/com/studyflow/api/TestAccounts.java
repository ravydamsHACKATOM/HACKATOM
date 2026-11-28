package com.studyflow.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.studyflow.auth.AuthInterceptor;
import jakarta.servlet.http.Cookie;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** Creates accounts for tests and hands back their session cookie. */
final class TestAccounts {

  static final String PASSWORD = "correct horse battery";

  private TestAccounts() {}

  static Cookie register(WebApplicationContext context, String email) throws Exception {
    String body =
        """
        {"name": "Ana", "email": "%s", "password": "%s"}
        """
            .formatted(email, PASSWORD);
    return MockMvcBuilders.webAppContextSetup(context)
        .build()
        .perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getCookie(AuthInterceptor.COOKIE);
  }
}
