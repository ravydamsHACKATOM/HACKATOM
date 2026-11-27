package com.studyflow.repository;

import com.studyflow.dto.RegisterRequest;
import com.studyflow.dto.ProfileUpdateRequest;
import com.studyflow.model.UserProfile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserRepository {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;

    public UserRepository(JdbcTemplate jdbc, PasswordEncoder passwordEncoder) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<UserProfile> findByEmail(String email) {
        return jdbc.query("SELECT id,name,email FROM users WHERE email=?",
                (rs, row) -> new UserProfile(rs.getLong("id"), rs.getString("name"),
                        rs.getString("email")), email).stream().findFirst();
    }

    public long create(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        jdbc.update("INSERT INTO users(name,email,password_hash) VALUES(?,?,?)",
                request.name().trim(), email, passwordEncoder.encode(request.password()));
        return jdbc.queryForObject("SELECT id FROM users WHERE email=?", Long.class, email);
    }

    public Optional<UserProfile> updateName(String email, ProfileUpdateRequest request) {
        jdbc.update("UPDATE users SET name=? WHERE email=?", request.name().trim(),
                email.toLowerCase());
        return findByEmail(email);
    }

    public long idByEmail(String email) {
        return jdbc.queryForObject("SELECT id FROM users WHERE email=?", Long.class,
                email.toLowerCase());
    }

    public boolean emailExists(String email) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE email=?",
                Integer.class, email.trim().toLowerCase());
        return count != null && count > 0;
    }

    public long userCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM users", Long.class);
    }

    public void assignLegacyRecords(long userId) {
        jdbc.update("UPDATE disciplines SET user_id=? WHERE user_id IS NULL", userId);
        jdbc.update("UPDATE study_events SET user_id=? WHERE user_id IS NULL", userId);
        jdbc.update("UPDATE availabilities SET user_id=? WHERE user_id IS NULL", userId);
        jdbc.update("UPDATE study_blocks SET user_id=? WHERE user_id IS NULL", userId);
    }
}
