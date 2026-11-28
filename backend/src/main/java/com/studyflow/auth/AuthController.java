package com.studyflow.auth;

import com.studyflow.auth.AuthService.Session;
import com.studyflow.common.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService auth;
  private final UserRepository users;

  public AuthController(AuthService auth, UserRepository users) {
    this.auth = auth;
    this.users = users;
  }

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  User register(
      @Valid @RequestBody AuthRequests.Register request,
      HttpServletRequest http,
      HttpServletResponse response) {
    return signIn(auth.register(request), http, response);
  }

  @PostMapping("/login")
  User login(
      @Valid @RequestBody AuthRequests.Login request,
      HttpServletRequest http,
      HttpServletResponse response) {
    return signIn(auth.login(request), http, response);
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void logout(HttpServletRequest http, HttpServletResponse response) {
    auth.logout(AuthInterceptor.sessionToken(http));
    response.addHeader(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO, http).toString());
  }

  @GetMapping("/me")
  User me(@RequestAttribute(AuthInterceptor.USER_ID) long userId) {
    return users.findById(userId).orElseThrow(() -> new NotFoundException("User", userId));
  }

  private User signIn(Session session, HttpServletRequest http, HttpServletResponse response) {
    response.addHeader(
        HttpHeaders.SET_COOKIE,
        cookie(session.token(), AuthService.SESSION_LIFETIME, http).toString());
    return session.user();
  }

  /**
   * HttpOnly keeps the token away from scripts and SameSite=Lax keeps other sites from sending it
   * on cross-site requests. Secure is set whenever the app is reached over HTTPS.
   */
  private static ResponseCookie cookie(String value, Duration maxAge, HttpServletRequest http) {
    return ResponseCookie.from(AuthInterceptor.COOKIE, value)
        .httpOnly(true)
        .secure(http.isSecure())
        .sameSite("Lax")
        .path("/")
        .maxAge(maxAge)
        .build();
  }
}
