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

  public Optional<StoredNote> find(LocalDate weekStart) {
    return jdbc.sql(
            "SELECT summary, tips, source, data_revision FROM plan_notes"
                + " WHERE week_start = :weekStart")
        .param("weekStart", weekStart.toString())
        .query(
            (rs, rowNum) ->
                new StoredNote(
                    new AdvisorNote(
                        rs.getString("summary"),
                        readTips(rs.getString("tips")),
                        rs.getString("source")),
                    rs.getLong("data_revision")))
        .optional();
  }

  public void save(LocalDate weekStart, AdvisorNote note, long dataRevision, LocalDateTime now) {
    jdbc.sql(
            "INSERT INTO plan_notes (week_start, summary, tips, source, created_at, data_revision)"
                + " VALUES (:weekStart, :summary, :tips, :source, :createdAt, :revision)"
                + " ON CONFLICT (week_start) DO UPDATE SET summary = excluded.summary,"
                + " tips = excluded.tips, source = excluded.source,"
                + " created_at = excluded.created_at, data_revision = excluded.data_revision")
        .param("weekStart", weekStart.toString())
        .param("summary", note.summary())
        .param("tips", writeTips(note.tips()))
        .param("source", note.source())
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
