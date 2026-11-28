package com.studyflow.availability;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class AvailabilityRepository {

  private static final String SELECT =
      "SELECT id, day_of_week, start_time, end_time FROM availability_windows"
          + " WHERE user_id = :userId";

  private final JdbcClient jdbc;

  public AvailabilityRepository(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  public List<AvailabilityWindow> findAll(long userId) {
    return jdbc.sql(SELECT + " ORDER BY day_of_week, start_time")
        .param("userId", userId)
        .query(AvailabilityRepository::map)
        .list();
  }

  public Optional<AvailabilityWindow> findById(long userId, long id) {
    return jdbc.sql(SELECT + " AND id = :id")
        .param("userId", userId)
        .param("id", id)
        .query(AvailabilityRepository::map)
        .optional();
  }

  /** True when another window of the same user and weekday overlaps the given range. */
  public boolean overlaps(long userId, AvailabilityRequest request, Long ignoreId) {
    return jdbc.sql(
                "SELECT COUNT(*) FROM availability_windows WHERE user_id = :userId"
                    + " AND day_of_week = :day AND start_time < :end AND end_time > :start"
                    + " AND id <> :ignoreId")
            .param("userId", userId)
            .param("day", request.dayOfWeek().getValue())
            .param("start", request.startTime().toString())
            .param("end", request.endTime().toString())
            .param("ignoreId", ignoreId == null ? -1L : ignoreId)
            .query(Integer.class)
            .single()
        > 0;
  }

  public AvailabilityWindow insert(long userId, AvailabilityRequest request) {
    KeyHolder keys = new GeneratedKeyHolder();
    jdbc.sql(
            "INSERT INTO availability_windows (user_id, day_of_week, start_time, end_time)"
                + " VALUES (:userId, :day, :start, :end)")
        .param("userId", userId)
        .param("day", request.dayOfWeek().getValue())
        .param("start", request.startTime().toString())
        .param("end", request.endTime().toString())
        .update(keys);
    return findById(userId, keys.getKey().longValue()).orElseThrow();
  }

  public boolean update(long userId, long id, AvailabilityRequest request) {
    return jdbc.sql(
                "UPDATE availability_windows SET day_of_week = :day, start_time = :start,"
                    + " end_time = :end WHERE id = :id AND user_id = :userId")
            .param("day", request.dayOfWeek().getValue())
            .param("start", request.startTime().toString())
            .param("end", request.endTime().toString())
            .param("id", id)
            .param("userId", userId)
            .update()
        > 0;
  }

  public boolean delete(long userId, long id) {
    return jdbc.sql("DELETE FROM availability_windows WHERE id = :id AND user_id = :userId")
            .param("id", id)
            .param("userId", userId)
            .update()
        > 0;
  }

  private static AvailabilityWindow map(ResultSet rs, int rowNum) throws SQLException {
    return new AvailabilityWindow(
        rs.getLong("id"),
        DayOfWeek.of(rs.getInt("day_of_week")),
        LocalTime.parse(rs.getString("start_time")),
        LocalTime.parse(rs.getString("end_time")));
  }
}
