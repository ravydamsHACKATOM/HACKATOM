package com.studyflow.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyflow.common.ApiError;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Guards the API: resolves the session cookie into a user and exposes its id to controllers as the
 * {@code userId} request attribute. Requests without a valid session get a 401.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

  public static final String COOKIE = "studyflow_session";
  public static final String USER_ID = "userId";

  private final AuthService auth;
  private final ObjectMapper json;

  public AuthInterceptor(AuthService auth, ObjectMapper json) {
    this.auth = auth;
    this.json = json;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws IOException {
    Optional<User> user = auth.authenticate(sessionToken(request));
    if (user.isPresent()) {
      request.setAttribute(USER_ID, user.get().id());
      return true;
    }
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    json.writeValue(
        response.getWriter(), ApiError.of("UNAUTHENTICATED", "Sign in to use this resource"));
    return false;
  }

  static String sessionToken(HttpServletRequest request) {
    if (request.getCookies() == null) {
      return null;
    }
    for (Cookie cookie : request.getCookies()) {
      if (COOKIE.equals(cookie.getName())) {
        return cookie.getValue();
      }
    }
    return null;
  }
}
