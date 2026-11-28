package com.studyflow.plan;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** How the student chose to organise each week: spread over the days and minutes per event. */
@Repository
public class WeekPreferencesRepository {

  /**
   * @param spread share the study time out over the days
   * @param allocations minutes chosen per event id; empty means the planner decides
   */
  public record WeekPreferences(boolean spread, Map<Long, Integer> allocations) {

    public static final WeekPreferences AUTOMATIC = new WeekPreferences(false, Map.of());
  }

  private final JdbcClient jdbc;

  public WeekPreferencesRepository(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  /** Allocations of events that no longer exist disappear with them (ON DELETE CASCADE). */
  public WeekPreferences find(long userId, LocalDate weekStart) {
    boolean spread =
        jdbc.sql(
                "SELECT spread FROM week_preferences"
                    + " WHERE user_id = :userId AND week_start = :weekStart")
            .param("userId", userId)
            .param("weekStart", weekStart.toString())
            .query(Integer.class)
            .optional()
            .map(value -> value == 1)
            .orElse(false);
    Map<Long, Integer> allocations = new LinkedHashMap<>();
    jdbc.sql(
            "SELECT a.event_id, a.minutes FROM week_allocations a"
                + " JOIN study_events e ON e.id = a.event_id"
                + " JOIN disciplines d ON d.id = e.discipline_id"
                + " WHERE a.user_id = :userId AND d.user_id = :userId"
                + " AND a.week_start = :weekStart ORDER BY a.event_id")
        .param("userId", userId)
        .param("weekStart", weekStart.toString())
        .query((rs, rowNum) -> allocations.put(rs.getLong("event_id"), rs.getInt("minutes")))
        .list();
    return new WeekPreferences(spread, allocations);
  }

  /** Replaces the preferences of the week. The event ids must belong to the user. */
  public void save(long userId, LocalDate weekStart, WeekPreferences preferences) {
    jdbc.sql(
            "INSERT INTO week_preferences (user_id, week_start, spread)"
                + " VALUES (:userId, :weekStart, :spread)"
                + " ON CONFLICT (user_id, week_start) DO UPDATE SET spread = excluded.spread")
        .param("userId", userId)
        .param("weekStart", weekStart.toString())
        .param("spread", preferences.spread() ? 1 : 0)
        .update();
    jdbc.sql("DELETE FROM week_allocations WHERE user_id = :userId AND week_start = :weekStart")
        .param("userId", userId)
        .param("weekStart", weekStart.toString())
        .update();
    preferences
        .allocations()
        .forEach(
            (eventId, minutes) ->
                jdbc.sql(
                        "INSERT INTO week_allocations (user_id, week_start, event_id, minutes)"
                            + " VALUES (:userId, :weekStart, :eventId, :minutes)")
                    .param("userId", userId)
                    .param("weekStart", weekStart.toString())
                    .param("eventId", eventId)
                    .param("minutes", minutes)
                    .update());
  }
}
