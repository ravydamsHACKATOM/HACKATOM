package com.example.studyFlow.dto;

import java.time.LocalDate;

public record EventResponse(Long id, String title, String type, Long subjectId, LocalDate dueDate, Double estimatedHours) {}
