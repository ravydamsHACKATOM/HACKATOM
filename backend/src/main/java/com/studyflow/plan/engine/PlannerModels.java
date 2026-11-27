package com.studyflow.plan.engine;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/** Plain data carried in and out of {@link SchedulePlanner}. No Spring, no database. */
public final class PlannerModels {

  private PlannerModels() {}

  /**
   * Work that still has to be scheduled for one exam or assignment.
   *
   * @param lastStudyDay last day on which a study block is still useful
   * @param remainingMinutes effort not yet covered by done or fixed blocks
   * @param weight discipline priority, 1 to 3, used to break ties
   * @param exam exams get their final block tagged as a review session
   */
  public record PlannerEvent(
      long id, LocalDate lastStudyDay, int remainingMinutes, int weight, boolean exam) {}

  /** A recurring weekly range in which the student can study. */
  public record WeeklyWindow(DayOfWeek day, LocalTime start, LocalTime end) {}

  /** A concrete range on a given date. */
  public record TimeRange(LocalDate date, LocalTime start, LocalTime end) {

    public int minutes() {
      return (int) Duration.between(start, end).toMinutes();
    }
  }

  /**
   * Everything the planner needs.
   *
   * @param weekStart Monday of the week being planned
   * @param from nothing is scheduled before this instant
   * @param busy ranges that are already taken (done, locked or skipped blocks)
   */
  public record PlannerInput(
      LocalDate weekStart,
      LocalDateTime from,
      List<WeeklyWindow> windows,
      List<TimeRange> busy,
      List<PlannerEvent> events) {

    public LocalDate weekEnd() {
      return weekStart.plusDays(6);
    }
  }

  public record PlannedBlock(
      long eventId, LocalDate date, LocalTime start, LocalTime end, boolean review) {

    public int minutes() {
      return (int) Duration.between(start, end).toMinutes();
    }
  }

  /** Effort that did not fit in the available time. */
  public record Shortfall(long eventId, int missingMinutes) {}

  /**
   * @param freeMinutes total free time the planner had to work with
   */
  public record PlannerResult(
      List<PlannedBlock> blocks, List<Shortfall> shortfalls, int freeMinutes) {}
}
