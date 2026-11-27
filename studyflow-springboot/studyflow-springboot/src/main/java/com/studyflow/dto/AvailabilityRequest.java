package com.studyflow.dto;

import jakarta.validation.constraints.*;
import java.time.LocalTime;

public record AvailabilityRequest(
        @Min(1) @Max(7) int dayOfWeek,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime
) {}
