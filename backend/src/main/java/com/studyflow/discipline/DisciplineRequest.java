package com.studyflow.discipline;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DisciplineRequest(
    @NotBlank @Size(max = 60) String name,
    @NotNull @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "must be a hex color like #3F6B5C")
        String color,
    @NotNull @Pattern(regexp = "^[a-z]{1,20}$", message = "must be a lowercase icon name")
        String icon,
    @NotNull @Min(1) @Max(3) Integer weight) {}
