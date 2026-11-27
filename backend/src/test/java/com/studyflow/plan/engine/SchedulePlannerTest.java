package com.studyflow.plan.engine;

import static org.assertj.core.api.Assertions.assertThat;

import com.studyflow.plan.engine.PlannerModels.PlannedBlock;
import com.studyflow.plan.engine.PlannerModels.PlannerEvent;
import com.studyflow.plan.engine.PlannerModels.PlannerInput;
import com.studyflow.plan.engine.PlannerModels.PlannerResult;
import com.studyflow.plan.engine.PlannerModels.Shortfall;
import com.studyflow.plan.engine.PlannerModels.TimeRange;
import com.studyflow.plan.engine.PlannerModels.WeeklyWindow;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class SchedulePlannerTest {

  private static final LocalDate MONDAY = LocalDate.of(2026, 11, 23);
  private static final LocalDateTime WEEK_OPEN = MONDAY.atStartOfDay();

  private final SchedulePlanner planner = new SchedulePlanner();

  @Test
  void placesBlocksOnlyInsideAvailability() {
    PlannerResult result =
        plan(
            WEEK_OPEN,
            List.of(window(DayOfWeek.MONDAY, "18:00", "20:00")),
            List.of(),
            List.of(assignment(1, MONDAY.plusDays(4), 240)));

    assertThat(result.blocks())
        .allSatisfy(
            block -> {
              assertThat(block.date()).isEqualTo(MONDAY);
              assertThat(block.start()).isAfterOrEqualTo(LocalTime.of(18, 0));
              assertThat(block.end()).isBeforeOrEqualTo(LocalTime.of(20, 0));
            });
  }

  @Test
  void splitsWorkIntoFocusSessionsWithBreaks() {
    PlannerResult result =
        plan(
            WEEK_OPEN,
            List.of(window(DayOfWeek.MONDAY, "18:00", "21:00")),
            List.of(),
            List.of(assignment(1, MONDAY.plusDays(4), 150)));

    assertThat(result.blocks())
        .extracting(PlannedBlock::start)
        .containsExactly(LocalTime.of(18, 0), LocalTime.of(19, 0), LocalTime.of(20, 0));
    assertThat(result.blocks()).allMatch(block -> block.minutes() == 50);
    assertThat(result.shortfalls()).isEmpty();
  }

  @Test
  void schedulesTheEarliestDeadlineFirst() {
    PlannerResult result =
        plan(
            WEEK_OPEN,
            List.of(window(DayOfWeek.MONDAY, "18:00", "19:00")),
            List.of(),
            List.of(assignment(1, MONDAY.plusDays(5), 50), assignment(2, MONDAY.plusDays(1), 50)));

    assertThat(result.blocks()).hasSize(1);
    assertThat(result.blocks().get(0).eventId()).isEqualTo(2);
  }

  @Test
  void neverStudiesForAnExamOnTheExamDay() {
    LocalDate examDay = MONDAY.plusDays(1);
    PlannerResult result =
        plan(
            WEEK_OPEN,
            List.of(
                window(DayOfWeek.MONDAY, "18:00", "19:00"),
                window(DayOfWeek.TUESDAY, "18:00", "21:00")),
            List.of(),
            List.of(exam(1, examDay, 150)));

    assertThat(result.blocks()).allMatch(block -> block.date().isBefore(examDay));
    assertThat(result.shortfalls()).containsExactly(new Shortfall(1, 100));
  }

  @Test
  void reportsWhatDoesNotFitInsteadOfSqueezingItIn() {
    PlannerResult result =
        plan(
            WEEK_OPEN,
            List.of(window(DayOfWeek.MONDAY, "18:00", "19:00")),
            List.of(),
            List.of(assignment(1, MONDAY.plusDays(2), 300)));

    assertThat(totalMinutes(result)).isEqualTo(50);
    assertThat(result.shortfalls()).containsExactly(new Shortfall(1, 250));
    assertThat(result.freeMinutes()).isEqualTo(60);
  }

  @Test
  void keepsBusyRangesFree() {
    TimeRange busy = new TimeRange(MONDAY, LocalTime.of(18, 0), LocalTime.of(18, 50));
    PlannerResult result =
        plan(
            WEEK_OPEN,
            List.of(window(DayOfWeek.MONDAY, "18:00", "20:00")),
            List.of(busy),
            List.of(assignment(1, MONDAY.plusDays(4), 100)));

    assertThat(result.blocks())
        .extracting(PlannedBlock::start)
        .containsExactly(LocalTime.of(19, 0));
  }

  @Test
  void neverSchedulesInThePast() {
    LocalDateTime wednesdayEvening = MONDAY.plusDays(2).atTime(18, 32);
    PlannerResult result =
        plan(
            wednesdayEvening,
            List.of(
                window(DayOfWeek.MONDAY, "18:00", "20:00"),
                window(DayOfWeek.WEDNESDAY, "18:00", "20:00")),
            List.of(),
            List.of(assignment(1, MONDAY.plusDays(4), 200)));

    assertThat(result.blocks())
        .allSatisfy(
            block -> {
              assertThat(block.date()).isEqualTo(MONDAY.plusDays(2));
              assertThat(block.start()).isAfterOrEqualTo(LocalTime.of(18, 35));
            });
  }

  @Test
  void takesOnlyAProportionalShareOfDeadlinesBeyondTheWeek() {
    LocalDate inThreeWeeks = MONDAY.plusDays(20);
    PlannerResult result =
        plan(
            WEEK_OPEN,
            everyWeekday("18:00", "21:00"),
            List.of(),
            List.of(assignment(1, inThreeWeeks, 600)));

    // 7 of 21 days fall in this week, so about a third of the 600 minutes.
    assertThat(totalMinutes(result)).isBetween(200, 225);
    assertThat(result.shortfalls()).isEmpty();
  }

  @Test
  void switchesSubjectsAfterTwoSessionsWhenThereIsSlack() {
    PlannerResult result =
        plan(
            WEEK_OPEN,
            List.of(
                window(DayOfWeek.MONDAY, "18:00", "22:00"),
                window(DayOfWeek.TUESDAY, "18:00", "22:00")),
            List.of(),
            List.of(
                assignment(1, MONDAY.plusDays(1), 200), assignment(2, MONDAY.plusDays(3), 100)));

    List<Long> mondayOrder =
        result.blocks().stream()
            .filter(block -> block.date().equals(MONDAY))
            .map(PlannedBlock::eventId)
            .toList();
    assertThat(mondayOrder).containsExactly(1L, 1L, 2L, 1L);
    assertThat(result.shortfalls()).isEmpty();
  }

  @Test
  void keepsCrammingWhenSwitchingWouldMissTheDeadline() {
    PlannerResult result =
        plan(
            WEEK_OPEN,
            List.of(window(DayOfWeek.MONDAY, "18:00", "22:00")),
            List.of(),
            List.of(assignment(1, MONDAY, 200), assignment(2, MONDAY.plusDays(3), 100)));

    assertThat(result.blocks()).extracting(PlannedBlock::eventId).containsOnly(1L);
    assertThat(result.shortfalls()).containsExactly(new Shortfall(2, 100));
  }

  @Test
  void tagsTheLastSessionBeforeAnExamAsReview() {
    PlannerResult result =
        plan(
            WEEK_OPEN,
            everyWeekday("18:00", "20:00"),
            List.of(),
            List.of(exam(1, MONDAY.plusDays(3), 150)));

    List<PlannedBlock> blocks = result.blocks();
    assertThat(blocks.get(blocks.size() - 1).review()).isTrue();
    assertThat(blocks.subList(0, blocks.size() - 1)).noneMatch(PlannedBlock::review);
  }

  @Test
  void usesShortLeftoversForTheTailOfAnEvent() {
    PlannerResult result =
        plan(
            WEEK_OPEN,
            List.of(window(DayOfWeek.MONDAY, "18:00", "20:00")),
            List.of(),
            List.of(assignment(1, MONDAY.plusDays(2), 70)));

    assertThat(result.blocks()).extracting(PlannedBlock::minutes).containsExactly(50, 20);
  }

  @Test
  void breaksDeadlineTiesByDisciplineWeight() {
    PlannerEvent light = new PlannerEvent(1, MONDAY.plusDays(2), 50, 1, false);
    PlannerEvent heavy = new PlannerEvent(2, MONDAY.plusDays(2), 50, 3, false);
    PlannerResult result =
        plan(
            WEEK_OPEN,
            List.of(window(DayOfWeek.MONDAY, "18:00", "19:00")),
            List.of(),
            List.of(light, heavy));

    assertThat(result.blocks().get(0).eventId()).isEqualTo(2);
  }

  @Test
  void returnsAnEmptyPlanWithoutAvailability() {
    PlannerResult result =
        plan(WEEK_OPEN, List.of(), List.of(), List.of(assignment(1, MONDAY.plusDays(2), 120)));

    assertThat(result.blocks()).isEmpty();
    assertThat(result.shortfalls()).containsExactly(new Shortfall(1, 120));
    assertThat(result.freeMinutes()).isZero();
  }

  private PlannerResult plan(
      LocalDateTime from,
      List<WeeklyWindow> windows,
      List<TimeRange> busy,
      List<PlannerEvent> events) {
    return planner.plan(new PlannerInput(MONDAY, from, windows, busy, events));
  }

  private static int totalMinutes(PlannerResult result) {
    return result.blocks().stream().mapToInt(PlannedBlock::minutes).sum();
  }

  private static WeeklyWindow window(DayOfWeek day, String start, String end) {
    return new WeeklyWindow(day, LocalTime.parse(start), LocalTime.parse(end));
  }

  private static List<WeeklyWindow> everyWeekday(String start, String end) {
    return List.of(
        window(DayOfWeek.MONDAY, start, end),
        window(DayOfWeek.TUESDAY, start, end),
        window(DayOfWeek.WEDNESDAY, start, end),
        window(DayOfWeek.THURSDAY, start, end),
        window(DayOfWeek.FRIDAY, start, end));
  }

  private static PlannerEvent assignment(long id, LocalDate dueDate, int minutes) {
    return new PlannerEvent(id, dueDate, minutes, 2, false);
  }

  private static PlannerEvent exam(long id, LocalDate examDate, int minutes) {
    return new PlannerEvent(id, examDate.minusDays(1), minutes, 2, true);
  }
}
