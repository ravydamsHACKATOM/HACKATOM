package com.studyflow.plan;

import com.studyflow.advisor.AdvisorNote;
import com.studyflow.event.EventType;
import java.time.LocalDate;
import java.util.List;

/**
 * Everything the UI needs to render one week.
 *
 * @param generated false while the student has not asked for a plan for this week yet
 * @param stale true when exams, assignments or availability changed after the plan was generated
 * @param changes what the last regeneration moved; only present right after generating
 * @param settings how the student chose to organise this week
 */
public record PlanView(
    LocalDate weekStart,
    LocalDate weekEnd,
    LocalDate today,
    boolean generated,
    boolean stale,
    List<StudyBlock> blocks,
    List<Warning> warnings,
    Stats stats,
    AdvisorNote note,
    Changes changes,
    Settings settings) {

  /**
   * @param spread the study time is shared out over the days
   * @param allocations minutes the student chose per event; empty when the planner decides
   */
  public record Settings(boolean spread, List<Allocation> allocations) {}

  public record Allocation(long eventId, int minutes) {}

  /** An exam or assignment whose effort does not fit before its deadline. */
  public record Warning(
      long eventId,
      String title,
      String disciplineName,
      EventType type,
      LocalDate dueDate,
      int missingMinutes) {}

  /**
   * @param availableMinutes free time in the week from now on, before any block is placed
   */
  public record Stats(
      int availableMinutes, int plannedMinutes, int doneMinutes, int missingMinutes) {}

  public record Changes(int added, int removed, int kept, List<EventDelta> events) {

    public boolean isEmpty() {
      return added == 0 && removed == 0;
    }
  }

  /** Minutes one event had in the week before and after a regeneration. */
  public record EventDelta(long eventId, String title, int beforeMinutes, int afterMinutes) {}
}
