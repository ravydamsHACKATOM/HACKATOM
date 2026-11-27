package com.studyflow.event;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/** Events belong to a discipline, and through it to a user: every query checks that owner. */
@Repository
public class StudyEventRepository {

  /** Minutes are derived from block times so they never drift from the schedule. */
  private static final String BLOCK_MINUTES =
      "(strftime('%s', '2000-01-01 ' || b.end_time)"
          + " - strftime('%s', '2000-01-01 ' || b.start_time)) / 60";

  private static final String SELECT =
      "SELECT e.id, e.discipline_id, d.name AS discipline_name, d.color AS discipline_color,"
          + " d.icon AS discipline_icon, e.title, e.type, e.due_date, e.effort_minutes, e.notes,"
          + " COALESCE((SELECT SUM("
          + BLOCK_MINUTES
          + ") FROM study_blocks b WHERE b.event_id = e.id AND b.status = 'DONE'), 0)"
          + " AS done_minutes,"
          + " COALESCE((SELECT SUM("
          + BLOCK_MINUTES
          + ") FROM study_blocks b WHERE b.event_id = e.id AND b.status = 'PLANNED'), 0)"
          + " AS planned_minutes"
          + " FROM study_events e JOIN disciplines d ON d.id = e.discipline_id"
          + " WHERE d.user_id = :userId";

  private static final String OWNED =
      " AND discipline_id IN (SELECT id FROM disciplines WHERE user_id = :userId)";

  private final JdbcClient jdbc;

  public StudyEventRepository(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  public List<StudyEvent> findAll(long userId) {
    return jdbc.sql(SELECT + " ORDER BY e.due_date, e.id")
        .param("userId", userId)
        .query(StudyEventRepository::map)
        .list();
  }

  public Optional<StudyEvent> findById(long userId, long id) {
    return jdbc.sql(SELECT + " AND e.id = :id")
        .param("userId", userId)
        .param("id", id)
        .query(StudyEventRepository::map)
        .optional();
  }

  /** The caller must have checked that the discipline belongs to the user. */
  public StudyEvent insert(long userId, StudyEventRequest request) {
    KeyHolder keys = new GeneratedKeyHolder();
    jdbc.sql(
            "INSERT INTO study_events (discipline_id, title, type, due_date, effort_minutes, notes)"
                + " VALUES (:disciplineId, :title, :type, :dueDate, :effortMinutes, :notes)")
        .param("disciplineId", request.disciplineId())
        .param("title", request.title().trim())
        .param("type", request.type().name())
        .param("dueDate", request.dueDate().toString())
        .param("effortMinutes", request.effortMinutes())
        .param("notes", request.notes())
        .update(keys);
    return findById(userId, keys.getKey().longValue()).orElseThrow();
  }

  public boolean update(long userId, long id, StudyEventRequest request) {
    return jdbc.sql(
                "UPDATE study_events SET discipline_id = :disciplineId, title = :title,"
                    + " type = :type, due_date = :dueDate, effort_minutes = :effortMinutes,"
                    + " notes = :notes WHERE id = :id"
                    + OWNED)
            .param("disciplineId", request.disciplineId())
            .param("title", request.title().trim())
            .param("type", request.type().name())
            .param("dueDate", request.dueDate().toString())
            .param("effortMinutes", request.effortMinutes())
            .param("notes", request.notes())
            .param("id", id)
            .param("userId", userId)
            .update()
        > 0;
  }

  public boolean delete(long userId, long id) {
    return jdbc.sql("DELETE FROM study_events WHERE id = :id" + OWNED)
            .param("id", id)
            .param("userId", userId)
            .update()
        > 0;
  }

  private static StudyEvent map(ResultSet rs, int rowNum) throws SQLException {
    return new StudyEvent(
        rs.getLong("id"),
        rs.getLong("discipline_id"),
        rs.getString("discipline_name"),
        rs.getString("discipline_color"),
        rs.getString("discipline_icon"),
        rs.getString("title"),
        EventType.valueOf(rs.getString("type")),
        LocalDate.parse(rs.getString("due_date")),
        rs.getInt("effort_minutes"),
        rs.getInt("done_minutes"),
        rs.getInt("planned_minutes"),
        rs.getString("notes"));
  }
}
