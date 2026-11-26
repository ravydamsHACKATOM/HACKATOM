package com.example.studyFlow.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.util.List;

public record AvailabilityRequest(
        @NotEmpty List<@NotNull Integer> daysOfWeek,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime
) {}
