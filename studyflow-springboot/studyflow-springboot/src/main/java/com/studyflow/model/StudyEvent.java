package com.studyflow.model;

public record StudyEvent(Long id, String title, String eventType, String dueDate,
                         double estimatedHours, Long disciplineId, String description) {}
