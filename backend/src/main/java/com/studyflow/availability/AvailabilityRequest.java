package com.studyflow.availability;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;

public record AvailabilityRequest(
    @NotNull DayOfWeek dayOfWeek, @NotNull LocalTime startTime, @NotNull LocalTime endTime) {

  static final int MIN_WINDOW_MINUTES = 25;

  @JsonIgnore
  @AssertTrue(message = "endTime must be at least 25 minutes after startTime")
  public boolean isLongEnough() {
    if (startTime == null || endTime == null) {
      return true;
    }
    return Duration.between(startTime, endTime).toMinutes() >= MIN_WINDOW_MINUTES;
  }
}
