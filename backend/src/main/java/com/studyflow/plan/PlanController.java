package com.studyflow.plan;

import com.studyflow.advisor.AdvisorService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
  private final AdvisorService advisor;
  private final Clock clock;

  public PlanController(PlanService plans, AdvisorService advisor, Clock clock) {
    this.plans = plans;
    this.advisor = advisor;
    this.clock = clock;
  }

  /** Plan of the week containing {@code date}; defaults to the current week. */
  @GetMapping("/plan")
  PlanView plan(@RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate date) {
    return plans.view(orToday(date));
  }

  @PostMapping("/plan/generate")
  PlanView generate(
      @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate date) {
    return plans.generate(orToday(date));
  }

  @PatchMapping("/blocks/{id}/done")
  StudyBlock done(@PathVariable long id, @Valid @RequestBody Toggle toggle) {
    return plans.setDone(id, toggle.value());
  }

  @PatchMapping("/blocks/{id}/locked")
  StudyBlock locked(@PathVariable long id, @Valid @RequestBody Toggle toggle) {
    return plans.setLocked(id, toggle.value());
  }

  @PostMapping("/blocks/{id}/postpone")
  PlanView postpone(@PathVariable long id) {
    return plans.postpone(id);
  }

  /** Lets the UI say whether explanations come from the AI or from the built-in rules. */
  @GetMapping("/advisor")
  Map<String, Object> advisor() {
    return Map.of("ai", advisor.aiEnabled());
  }

  private LocalDate orToday(LocalDate date) {
    return date != null ? date : LocalDate.now(clock);
  }
}
