package com.studyflow.plan.engine;

import com.studyflow.plan.engine.PlannerModels.PlannedBlock;
import com.studyflow.plan.engine.PlannerModels.PlannerEvent;
import com.studyflow.plan.engine.PlannerModels.PlannerInput;
import com.studyflow.plan.engine.PlannerModels.PlannerResult;
import com.studyflow.plan.engine.PlannerModels.Shortfall;
import com.studyflow.plan.engine.PlannerModels.TimeRange;
import com.studyflow.plan.engine.PlannerModels.WeeklyWindow;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Deterministic weekly scheduler.
 *
 * <p>The rules, in order:
 *
 * <ol>
 *   <li>Blocks only go inside the student's availability, never over a busy range and never in the
 *       past.
 *   <li>Work happens in focus sessions of up to {@value #FOCUS_MINUTES} minutes separated by
 *       {@value #BREAK_MINUTES}-minute breaks.
 *   <li>The most urgent deadline goes first (earliest deadline first). This is what guarantees
 *       that, if the week can fit everything, nothing is left out.
 *   <li>After two sessions in a row of the same subject the planner switches to another one, but
 *       only when the urgent subject still has enough slack to finish on time.
 *   <li>A deadline beyond this week only takes its proportional share of the week.
 *   <li>What does not fit is reported as a shortfall instead of being squeezed in.
 * </ol>
 */
public class SchedulePlanner {

  public static final int FOCUS_MINUTES = 50;
  public static final int BREAK_MINUTES = 10;
  public static final int MIN_BLOCK_MINUTES = 25;
  private static final int SHORTEST_TAIL_MINUTES = 15;
  private static final int MAX_CONSECUTIVE_SESSIONS = 2;

  public PlannerResult plan(PlannerInput input) {
    List<TimeRange> free = freeRanges(input);
    Map<Long, Integer> quota = weeklyQuota(input);
    Map<Long, PlannerEvent> events = new HashMap<>();
    input.events().forEach(event -> events.put(event.id(), event));

    List<PlannedBlock> blocks = new ArrayList<>();
    for (int index = 0; index < free.size(); index++) {
      TimeRange range = free.get(index);
      LocalTime cursor = range.start();
      Long previous = null;
      int streak = 0;

      while (minutesBetween(cursor, range.end()) >= MIN_BLOCK_MINUTES) {
        List<PlannerEvent> candidates = candidates(events, quota, range.date());
        if (candidates.isEmpty()) {
          break;
        }
        PlannerEvent chosen = candidates.get(0);
        boolean streakTooLong =
            chosen.id() == (previous == null ? -1 : previous) && streak >= MAX_CONSECUTIVE_SESSIONS;
        if (streakTooLong && candidates.size() > 1) {
          LocalTime afterNext = cursor.plusMinutes(FOCUS_MINUTES + BREAK_MINUTES);
          int slack =
              focusCapacity(free, index, afterNext, chosen.lastStudyDay()) - quota.get(chosen.id());
          if (slack >= 0) {
            chosen = candidates.get(1);
          }
        }

        int available = minutesBetween(cursor, range.end());
        int wanted = Math.max(SHORTEST_TAIL_MINUTES, roundUpToFive(quota.get(chosen.id())));
        int length = Math.min(FOCUS_MINUTES, Math.min(available, wanted));
        blocks.add(
            new PlannedBlock(chosen.id(), range.date(), cursor, cursor.plusMinutes(length), false));
        quota.merge(chosen.id(), -length, (left, used) -> Math.max(0, left + used));

        streak = previous != null && previous == chosen.id() ? streak + 1 : 1;
        previous = chosen.id();
        if (available - length < BREAK_MINUTES) {
          break;
        }
        cursor = cursor.plusMinutes(length + BREAK_MINUTES);
      }
    }

    List<Shortfall> shortfalls =
        quota.entrySet().stream()
            .filter(entry -> entry.getValue() > 0)
            .map(entry -> new Shortfall(entry.getKey(), entry.getValue()))
            .toList();
    int freeMinutes = free.stream().mapToInt(TimeRange::minutes).sum();
    return new PlannerResult(tagReviews(blocks, events, quota, input), shortfalls, freeMinutes);
  }

  /** Availability from {@code input.from()} to the end of the week, minus the busy ranges. */
  public List<TimeRange> freeRanges(PlannerInput input) {
    LocalDate firstDay = firstDay(input);
    LocalTime earliestToday = roundUpToFive(input.from().toLocalTime());
    List<TimeRange> free = new ArrayList<>();

    for (LocalDate day = firstDay; !day.isAfter(input.weekEnd()); day = day.plusDays(1)) {
      LocalDate date = day;
      List<TimeRange> busy =
          input.busy().stream()
              .filter(range -> range.date().equals(date))
              .sorted(Comparator.comparing(TimeRange::start))
              .toList();
      List<WeeklyWindow> windows =
          input.windows().stream()
              .filter(window -> window.day() == date.getDayOfWeek())
              .sorted(Comparator.comparing(WeeklyWindow::start))
              .toList();

      for (WeeklyWindow window : windows) {
        LocalTime start = window.start();
        boolean isFromDay = date.equals(input.from().toLocalDate());
        if (isFromDay && earliestToday == null) {
          continue; // rounding crossed midnight: nothing left today
        }
        if (isFromDay && start.isBefore(earliestToday)) {
          start = earliestToday;
        }
        for (TimeRange taken : busy) {
          if (!taken.end().isAfter(start) || !taken.start().isBefore(window.end())) {
            continue;
          }
          addIfUsable(free, date, start, taken.start());
          LocalTime resume = plusMinutesSameDay(taken.end(), BREAK_MINUTES);
          start = resume == null ? window.end() : max(start, resume);
        }
        addIfUsable(free, date, start, window.end());
      }
    }
    return free;
  }

  /** How much of each event should be scheduled in this week. */
  private Map<Long, Integer> weeklyQuota(PlannerInput input) {
    LocalDate firstDay = firstDay(input);
    long daysLeftInWeek = ChronoUnit.DAYS.between(firstDay, input.weekEnd()) + 1;
    Map<Long, Integer> quota = new LinkedHashMap<>();

    input.events().stream()
        .sorted(urgency())
        .forEach(
            event -> {
              if (event.remainingMinutes() <= 0) {
                return;
              }
              if (!event.lastStudyDay().isAfter(input.weekEnd())) {
                quota.put(event.id(), event.remainingMinutes());
                return;
              }
              long daysUntilDeadline = ChronoUnit.DAYS.between(firstDay, event.lastStudyDay()) + 1;
              int share =
                  (int)
                      Math.ceil(
                          event.remainingMinutes() * (double) daysLeftInWeek / daysUntilDeadline);
              int rounded = (int) Math.ceil(share / (double) MIN_BLOCK_MINUTES) * MIN_BLOCK_MINUTES;
              quota.put(event.id(), Math.min(event.remainingMinutes(), rounded));
            });
    return quota;
  }

  private static List<PlannerEvent> candidates(
      Map<Long, PlannerEvent> events, Map<Long, Integer> quota, LocalDate date) {
    return quota.entrySet().stream()
        .filter(entry -> entry.getValue() > 0)
        .map(entry -> events.get(entry.getKey()))
        .filter(event -> !event.lastStudyDay().isBefore(date))
        .sorted(urgency())
        .toList();
  }

  private static Comparator<PlannerEvent> urgency() {
    return Comparator.comparing(PlannerEvent::lastStudyDay)
        .thenComparing(Comparator.comparingInt(PlannerEvent::weight).reversed())
        .thenComparingLong(PlannerEvent::id);
  }

  /** Focus minutes that still fit from the given point up to {@code until}, inclusive. */
  private static int focusCapacity(
      List<TimeRange> free, int index, LocalTime cursor, LocalDate until) {
    int capacity = 0;
    for (int i = index; i < free.size(); i++) {
      TimeRange range = free.get(i);
      if (range.date().isAfter(until)) {
        break;
      }
      LocalTime start = i == index ? cursor : range.start();
      if (start == null || !start.isBefore(range.end())) {
        continue;
      }
      int minutes = minutesBetween(start, range.end());
      int sessions = (minutes + BREAK_MINUTES) / (FOCUS_MINUTES + BREAK_MINUTES);
      int tail = minutes - sessions * (FOCUS_MINUTES + BREAK_MINUTES);
      capacity += sessions * FOCUS_MINUTES + (tail >= MIN_BLOCK_MINUTES ? tail : 0);
    }
    return capacity;
  }

  /** The last block of a fully scheduled exam becomes a review session. */
  private static List<PlannedBlock> tagReviews(
      List<PlannedBlock> blocks,
      Map<Long, PlannerEvent> events,
      Map<Long, Integer> quota,
      PlannerInput input) {
    Map<Long, Integer> lastIndex = new HashMap<>();
    Map<Long, Integer> count = new HashMap<>();
    for (int i = 0; i < blocks.size(); i++) {
      lastIndex.put(blocks.get(i).eventId(), i);
      count.merge(blocks.get(i).eventId(), 1, Integer::sum);
    }
    List<PlannedBlock> tagged = new ArrayList<>(blocks);
    lastIndex.forEach(
        (eventId, index) -> {
          PlannerEvent event = events.get(eventId);
          boolean fullyScheduled = quota.getOrDefault(eventId, 0) == 0;
          boolean dueThisWeek = !event.lastStudyDay().isAfter(input.weekEnd());
          if (event.exam() && fullyScheduled && dueThisWeek && count.get(eventId) >= 2) {
            PlannedBlock last = blocks.get(index);
            tagged.set(
                index,
                new PlannedBlock(last.eventId(), last.date(), last.start(), last.end(), true));
          }
        });
    return tagged;
  }

  private static LocalDate firstDay(PlannerInput input) {
    LocalDate fromDay = input.from().toLocalDate();
    return fromDay.isAfter(input.weekStart()) ? fromDay : input.weekStart();
  }

  private static void addIfUsable(
      List<TimeRange> free, LocalDate date, LocalTime start, LocalTime end) {
    if (start.isBefore(end) && minutesBetween(start, end) >= MIN_BLOCK_MINUTES) {
      free.add(new TimeRange(date, start, end));
    }
  }

  private static int minutesBetween(LocalTime start, LocalTime end) {
    return (int) ChronoUnit.MINUTES.between(start, end);
  }

  private static int roundUpToFive(int minutes) {
    return (int) Math.ceil(minutes / 5.0) * 5;
  }

  /** Rounds up to the next 5-minute mark; null when that would cross midnight. */
  private static LocalTime roundUpToFive(LocalTime time) {
    int minuteOfDay = time.toSecondOfDay() / 60 + (time.getSecond() > 0 ? 1 : 0);
    int rounded = roundUpToFive(minuteOfDay);
    return rounded >= 24 * 60 ? null : LocalTime.ofSecondOfDay(rounded * 60L);
  }

  private static LocalTime plusMinutesSameDay(LocalTime time, int minutes) {
    int minuteOfDay = time.toSecondOfDay() / 60 + minutes;
    return minuteOfDay >= 24 * 60 ? null : LocalTime.ofSecondOfDay(minuteOfDay * 60L);
  }

  private static LocalTime max(LocalTime a, LocalTime b) {
    return a.isAfter(b) ? a : b;
  }
}
