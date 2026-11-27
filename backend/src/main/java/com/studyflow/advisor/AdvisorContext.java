package com.studyflow.advisor;

import com.studyflow.plan.PlanView;
import java.time.LocalDate;
import java.util.List;

/**
 * Facts about a freshly generated plan. Advisors only describe these facts; they never change the
 * schedule itself.
 *
 * @param replan true when a previous plan for the same week was replaced
 */
public record AdvisorContext(
    LocalDate weekStart,
    LocalDate today,
    boolean replan,
    PlanView.Stats stats,
    List<PlanView.Warning> warnings,
    PlanView.Changes changes,
    List<DayLoad> days,
    List<EventProgress> events) {

  /** Planned minutes on one day of the week. */
  public record DayLoad(LocalDate date, int plannedMinutes, int blocks) {}

  /** How one exam or assignment is covered by the plan. */
  public record EventProgress(
      String title,
      String discipline,
      String type,
      LocalDate dueDate,
      int effortMinutes,
      int doneMinutes,
      int plannedThisWeekMinutes) {}
}
