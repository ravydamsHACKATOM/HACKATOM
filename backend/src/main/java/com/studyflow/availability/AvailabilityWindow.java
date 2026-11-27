package com.studyflow.availability;

import java.time.DayOfWeek;
import java.time.LocalTime;

/** A recurring weekly time range in which the student can really study. */
public record AvailabilityWindow(
    long id, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {}
