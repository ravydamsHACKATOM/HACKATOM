package com.studyflow.auth;

import com.studyflow.auth.UserRepository.Credentials;
import com.studyflow.common.BusinessRuleException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Accounts and sessions.
 *
 * <p>Passwords are stored as BCrypt hashes. A session is a random 256-bit token kept in an HttpOnly
 * cookie; the database only stores its SHA-256 digest, so a leaked database cannot be used to
 * impersonate anyone.
 */
@Service
public class AuthService {

  public static final Duration SESSION_LIFETIME = Duration.ofDays(14);

  /** A freshly created session: the user and the raw token to put in the cookie. */
  public record Session(User user, String token) {}

  private final UserRepository users;
  private final Clock clock;
  private final PasswordEncoder passwords = new BCryptPasswordEncoder();
  private final SecureRandom random = new SecureRandom();

  public AuthService(UserRepository users, Clock clock) {
    this.users = users;
    this.clock = clock;
  }

  @Transactional
  public Session register(AuthRequests.Register request) {
    String email = normalize(request.email());
    if (users.existsByEmail(email)) {
      throw new BusinessRuleException("EMAIL_TAKEN", "There is already an account with this email");
    }
    User user =
        users.insert(
            request.name().trim(),
            email,
            passwords.encode(request.password()),
            LocalDateTime.now(clock));
    return openSession(user);
  }

  @Transactional
  public Session login(AuthRequests.Login request) {
    Optional<Credentials> credentials = users.findCredentials(normalize(request.email()));
    // The same answer for an unknown email and a wrong password, so accounts cannot be probed.
    if (credentials.isEmpty()
        || !passwords.matches(request.password(), credentials.get().passwordHash())) {
      throw new InvalidCredentialsException();
    }
    return openSession(credentials.get().user());
  }

  public void logout(String token) {
    if (token != null) {
      users.deleteSession(digest(token));
    }
  }

  /** The user behind a session token, if the session exists and has not expired. */
  public Optional<User> authenticate(String token) {
    if (token == null || token.isBlank()) {
      return Optional.empty();
    }
    return users.findSessionUser(digest(token), LocalDateTime.now(clock)).flatMap(users::findById);
  }

  private Session openSession(User user) {
    LocalDateTime now = LocalDateTime.now(clock);
    users.deleteExpiredSessions(now);
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    users.saveSession(digest(token), user.id(), now.plus(SESSION_LIFETIME));
    return new Session(user, token);
  }

  private static String normalize(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }

  private static String digest(String token) {
    try {
      MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(sha256.digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is required by every Java runtime", e);
    }
  }
}
