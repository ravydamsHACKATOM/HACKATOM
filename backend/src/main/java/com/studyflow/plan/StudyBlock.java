package com.studyflow.plan;

import com.studyflow.event.EventType;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

/** A scheduled study session, already joined with what the UI needs to render it. */
public record StudyBlock(
    long id,
    long eventId,
    String eventTitle,
    EventType eventType,
    LocalDate dueDate,
    String disciplineName,
    String disciplineColor,
    String disciplineIcon,
    LocalDate date,
    LocalTime startTime,
    LocalTime endTime,
    BlockKind kind,
    BlockStatus status,
    boolean locked) {

  public int getMinutes() {
    return (int) Duration.between(startTime, endTime).toMinutes();
  }
}
