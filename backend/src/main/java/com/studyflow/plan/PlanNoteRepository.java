package com.studyflow.plan;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyflow.advisor.AdvisorNote;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Stores the advisor note of each generated week and the data revision it was built from. */
@Repository
public class PlanNoteRepository {

  /** A stored note plus the revision used to detect a stale plan. */
  public record StoredNote(AdvisorNote note, long dataRevision) {}

  private static final TypeReference<List<String>> TIPS = new TypeReference<>() {};

  private final JdbcClient jdbc;
  private final ObjectMapper json;

  public PlanNoteRepository(JdbcClient jdbc, ObjectMapper json) {
    this.jdbc = jdbc;
    this.json = json;
  }

  public Optional<StoredNote> find(long userId, LocalDate weekStart) {
    return jdbc.sql(
            "SELECT summary, tips, data_revision FROM plan_notes"
                + " WHERE user_id = :userId AND week_start = :weekStart")
        .param("userId", userId)
        .param("weekStart", weekStart.toString())
        .query(
            (rs, rowNum) ->
                new StoredNote(
                    new AdvisorNote(rs.getString("summary"), readTips(rs.getString("tips"))),
                    rs.getLong("data_revision")))
        .optional();
  }

  public void save(
      long userId, LocalDate weekStart, AdvisorNote note, long dataRevision, LocalDateTime now) {
    jdbc.sql(
            "INSERT INTO plan_notes (user_id, week_start, summary, tips, created_at, data_revision)"
                + " VALUES (:userId, :weekStart, :summary, :tips, :createdAt, :revision)"
                + " ON CONFLICT (user_id, week_start) DO UPDATE SET summary = excluded.summary,"
                + " tips = excluded.tips, created_at = excluded.created_at,"
                + " data_revision = excluded.data_revision")
        .param("userId", userId)
        .param("weekStart", weekStart.toString())
        .param("summary", note.summary())
        .param("tips", writeTips(note.tips()))
        .param("createdAt", now.toString())
        .param("revision", dataRevision)
        .update();
  }

  private List<String> readTips(String value) {
    try {
      return json.readValue(value, TIPS);
    } catch (JsonProcessingException e) {
      return List.of();
    }
  }

  private String writeTips(List<String> tips) {
    try {
      return json.writeValueAsString(tips);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Could not serialize advisor tips", e);
    }
  }
}
