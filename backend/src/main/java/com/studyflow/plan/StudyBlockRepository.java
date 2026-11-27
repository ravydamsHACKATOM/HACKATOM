package com.studyflow.plan;

import com.studyflow.event.EventType;
import com.studyflow.plan.engine.PlannerModels.PlannedBlock;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class StudyBlockRepository {

  private static final String SELECT =
      "SELECT b.id, b.event_id, e.title AS event_title, e.type AS event_type, e.due_date,"
          + " d.name AS discipline_name, d.color AS discipline_color, d.icon AS discipline_icon,"
          + " b.block_date, b.start_time, b.end_time, b.kind, b.status, b.locked"
          + " FROM study_blocks b"
          + " JOIN study_events e ON e.id = b.event_id"
          + " JOIN disciplines d ON d.id = e.discipline_id";

  private final JdbcClient jdbc;

  public StudyBlockRepository(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  public List<StudyBlock> findBetween(LocalDate from, LocalDate to) {
    return jdbc.sql(
            SELECT
                + " WHERE b.block_date BETWEEN :from AND :to"
                + " ORDER BY b.block_date, b.start_time")
        .param("from", from.toString())
        .param("to", to.toString())
        .query(StudyBlockRepository::map)
        .list();
  }

  public Optional<StudyBlock> findById(long id) {
    return jdbc.sql(SELECT + " WHERE b.id = :id")
        .param("id", id)
        .query(StudyBlockRepository::map)
        .optional();
  }

  public void insertAll(List<PlannedBlock> blocks) {
    for (PlannedBlock block : blocks) {
      jdbc.sql(
              "INSERT INTO study_blocks"
                  + " (event_id, block_date, start_time, end_time, kind, status, locked)"
                  + " VALUES (:eventId, :date, :start, :end, :kind, 'PLANNED', 0)")
          .param("eventId", block.eventId())
          .param("date", block.date().toString())
          .param("start", block.start().toString())
          .param("end", block.end().toString())
          .param("kind", (block.review() ? BlockKind.REVIEW : BlockKind.FOCUS).name())
          .update();
    }
  }

  public void deleteAll(List<Long> ids) {
    if (!ids.isEmpty()) {
      jdbc.sql("DELETE FROM study_blocks WHERE id IN (:ids)").param("ids", ids).update();
    }
  }

  /** Planned blocks whose time has passed go back to the pool of pending effort. */
  public int markMissedBefore(LocalDate date, LocalTime time) {
    return jdbc.sql(
            "UPDATE study_blocks SET status = 'MISSED', locked = 0 WHERE status = 'PLANNED'"
                + " AND (block_date < :date OR (block_date = :date AND end_time <= :time))")
        .param("date", date.toString())
        .param("time", time.toString())
        .update();
  }

  public void updateStatus(long id, BlockStatus status) {
    jdbc.sql("UPDATE study_blocks SET status = :status WHERE id = :id")
        .param("status", status.name())
        .param("id", id)
        .update();
  }

  public void updateLocked(long id, boolean locked) {
    jdbc.sql("UPDATE study_blocks SET locked = :locked WHERE id = :id")
        .param("locked", locked ? 1 : 0)
        .param("id", id)
        .update();
  }

  private static StudyBlock map(ResultSet rs, int rowNum) throws SQLException {
    return new StudyBlock(
        rs.getLong("id"),
        rs.getLong("event_id"),
        rs.getString("event_title"),
        EventType.valueOf(rs.getString("event_type")),
        LocalDate.parse(rs.getString("due_date")),
        rs.getString("discipline_name"),
        rs.getString("discipline_color"),
        rs.getString("discipline_icon"),
        LocalDate.parse(rs.getString("block_date")),
        LocalTime.parse(rs.getString("start_time")),
        LocalTime.parse(rs.getString("end_time")),
        BlockKind.valueOf(rs.getString("kind")),
        BlockStatus.valueOf(rs.getString("status")),
        rs.getInt("locked") == 1);
  }
}
