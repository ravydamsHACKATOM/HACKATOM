package com.studyflow.auth;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {

  /** An account together with its password hash, used only to check a login. */
  record Credentials(User user, String passwordHash) {}

  private final JdbcClient jdbc;

  public UserRepository(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  Optional<User> findById(long id) {
    return jdbc.sql("SELECT id, name, email FROM users WHERE id = :id")
        .param("id", id)
        .query(User.class)
        .optional();
  }

  Optional<Credentials> findCredentials(String email) {
    return jdbc.sql("SELECT id, name, email, password_hash FROM users WHERE email = :email")
        .param("email", email)
        .query(
            (rs, rowNum) ->
                new Credentials(
                    new User(rs.getLong("id"), rs.getString("name"), rs.getString("email")),
                    rs.getString("password_hash")))
        .optional();
  }

  boolean existsByEmail(String email) {
    return jdbc.sql("SELECT COUNT(*) FROM users WHERE email = :email")
            .param("email", email)
            .query(Integer.class)
            .single()
        > 0;
  }

  User insert(String name, String email, String passwordHash, LocalDateTime now) {
    KeyHolder keys = new GeneratedKeyHolder();
    jdbc.sql(
            "INSERT INTO users (name, email, password_hash, created_at)"
                + " VALUES (:name, :email, :hash, :now)")
        .param("name", name)
        .param("email", email)
        .param("hash", passwordHash)
        .param("now", now.toString())
        .update(keys);
    return new User(keys.getKey().longValue(), name, email);
  }

  void saveSession(String tokenHash, long userId, LocalDateTime expiresAt) {
    jdbc.sql(
            "INSERT INTO sessions (token_hash, user_id, expires_at)"
                + " VALUES (:hash, :userId, :expiresAt)")
        .param("hash", tokenHash)
        .param("userId", userId)
        .param("expiresAt", expiresAt.toString())
        .update();
  }

  Optional<Long> findSessionUser(String tokenHash, LocalDateTime now) {
    return jdbc.sql("SELECT user_id FROM sessions WHERE token_hash = :hash AND expires_at > :now")
        .param("hash", tokenHash)
        .param("now", now.toString())
        .query(Long.class)
        .optional();
  }

  void deleteSession(String tokenHash) {
    jdbc.sql("DELETE FROM sessions WHERE token_hash = :hash").param("hash", tokenHash).update();
  }

  void deleteExpiredSessions(LocalDateTime now) {
    jdbc.sql("DELETE FROM sessions WHERE expires_at <= :now").param("now", now.toString()).update();
  }
}
