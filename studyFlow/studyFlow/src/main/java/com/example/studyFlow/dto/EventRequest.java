package com.example.studyFlow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record EventRequest(
        @NotBlank @Size(max = 150) String title,
        @NotBlank @Size(max = 30) String type,
        @NotNull @Positive Long subjectId,
        @NotNull LocalDate dueDate,
        @NotNull @Positive Double estimatedHours
) {}
