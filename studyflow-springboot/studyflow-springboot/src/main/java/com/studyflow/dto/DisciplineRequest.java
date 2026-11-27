package com.studyflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DisciplineRequest(
        @NotBlank @Size(max = 80) String name,
        @Size(max = 20) String color,
        @Size(max = 500) String description
) {}
