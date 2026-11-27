package com.studyflow.event;

import java.time.LocalDate;

/**
 * An exam or assignment the student has to prepare for.
 *
 * @param effortMinutes total study time the student estimates
 * @param doneMinutes time already covered by completed study blocks
 * @param plannedMinutes time covered by blocks that are still planned
 */
public record StudyEvent(
    long id,
    long disciplineId,
    String disciplineName,
    String disciplineColor,
    String disciplineIcon,
    String title,
    EventType type,
    LocalDate dueDate,
    int effortMinutes,
    int doneMinutes,
    int plannedMinutes,
    String notes) {

  /** Last day on which studying for this event still makes sense. */
  public LocalDate lastStudyDay() {
    return type == EventType.EXAM ? dueDate.minusDays(1) : dueDate;
  }

  public int remainingMinutes() {
    return Math.max(0, effortMinutes - doneMinutes);
  }
}
