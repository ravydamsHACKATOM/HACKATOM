package com.studyflow.model;

public record StudyBlock(Long id, Long studyEventId, Long disciplineId, String studyDate,
                         String startTime, String endTime, int durationMinutes, boolean completed) {}
