package com.studyflow.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record StudyEventRequest(
        @NotBlank @Size(max = 120) String title,
        @NotBlank @Pattern(regexp = "EXAM|ASSIGNMENT") String eventType,
        @NotNull LocalDate dueDate,
        @DecimalMin("0.5") @DecimalMax("300") double estimatedHours,
        @NotNull @Positive Long disciplineId,
        @Size(max = 1000) String description
) {}
