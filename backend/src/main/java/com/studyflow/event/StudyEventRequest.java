package com.studyflow.event;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public record StudyEventRequest(
    @NotNull Long disciplineId,
    @NotBlank @Size(max = 80) String title,
    @NotNull EventType type,
    @NotNull LocalDate dueDate,
    @NotNull @DecimalMin("0.25") @DecimalMax("80") BigDecimal effortHours,
    @Size(max = 500) String notes) {

  int effortMinutes() {
    return effortHours
        .multiply(BigDecimal.valueOf(60))
        .setScale(0, RoundingMode.HALF_UP)
        .intValue();
  }
}
