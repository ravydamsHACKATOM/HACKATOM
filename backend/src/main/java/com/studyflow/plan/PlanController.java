package com.studyflow.plan;

import com.studyflow.auth.AuthInterceptor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PlanController {

  /** Body of the PATCH endpoints: a single on/off switch. */
  public record Toggle(@NotNull Boolean value) {}

  private final PlanService plans;
  private final Clock clock;

  public PlanController(PlanService plans, Clock clock) {
    this.plans = plans;
    this.clock = clock;
  }

  /** Plan of the week containing {@code date}; defaults to the current week. */
  @GetMapping("/plan")
  PlanView plan(
      @RequestAttribute(AuthInterceptor.USER_ID) long userId,
      @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate date) {
    return plans.view(userId, orToday(date));
  }

  @PostMapping("/plan/generate")
  PlanView generate(
      @RequestAttribute(AuthInterceptor.USER_ID) long userId,
      @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate date) {
    return plans.generate(userId, orToday(date));
  }

  @PatchMapping("/blocks/{id}/done")
  StudyBlock done(
      @RequestAttribute(AuthInterceptor.USER_ID) long userId,
      @PathVariable long id,
      @Valid @RequestBody Toggle toggle) {
    return plans.setDone(userId, id, toggle.value());
  }

  @PatchMapping("/blocks/{id}/locked")
  StudyBlock locked(
      @RequestAttribute(AuthInterceptor.USER_ID) long userId,
      @PathVariable long id,
      @Valid @RequestBody Toggle toggle) {
    return plans.setLocked(userId, id, toggle.value());
  }

  @PostMapping("/blocks/{id}/postpone")
  PlanView postpone(@RequestAttribute(AuthInterceptor.USER_ID) long userId, @PathVariable long id) {
    return plans.postpone(userId, id);
  }

  private LocalDate orToday(LocalDate date) {
    return date != null ? date : LocalDate.now(clock);
  }
}
