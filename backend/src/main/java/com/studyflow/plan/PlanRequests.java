package com.studyflow.plan;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Bodies of the endpoints that reorganise a week. */
public final class PlanRequests {

  private PlanRequests() {}

  /**
   * How the student wants the week organised.
   *
   * @param spread share the study time out over the days
   * @param allocations minutes per exam or assignment this week; an empty list means automatic
   */
  public record SettingsRequest(
      @NotNull Boolean spread, @NotNull List<@Valid @NotNull AllocationRequest> allocations) {}

  /** At most 100 hours per event and week, which no real week gets close to. */
  public record AllocationRequest(
      @NotNull Long eventId, @NotNull @Min(0) @Max(6000) Integer minutes) {}

  /** New day and start of a study block. Its length stays the same. */
  public record MoveRequest(@NotNull LocalDate date, @NotNull LocalTime startTime) {}
}
