package com.example.studyFlow.dto;

import java.time.LocalTime;
import java.util.List;

public record AvailabilityResponse(List<Integer> daysOfWeek, LocalTime startTime, LocalTime endTime) {}
