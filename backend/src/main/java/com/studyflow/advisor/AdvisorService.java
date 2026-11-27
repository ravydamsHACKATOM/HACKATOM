package com.studyflow.advisor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Picks who explains the plan: the AI when a key is configured, the fixed rules otherwise. Any
 * failure of the AI (timeout, refusal, bad answer, quota) silently falls back to the rules, so
 * generating a plan never fails because of the AI provider.
 */
@Service
public class AdvisorService {

  private static final Logger log = LoggerFactory.getLogger(AdvisorService.class);

  private final PlanAdvisor fallback;
  private final PlanAdvisor ai;

  @Autowired
  public AdvisorService(
      RuleBasedAdvisor fallback, AiProperties properties, ObjectMapper objectMapper) {
    this(fallback, properties.enabled() ? new ClaudeAdvisor(properties, objectMapper) : null);
    log.info(
        "Plan advisor: {}", properties.enabled() ? "AI (" + properties.model() + ")" : "rules");
  }

  AdvisorService(PlanAdvisor fallback, PlanAdvisor ai) {
    this.fallback = fallback;
    this.ai = ai;
  }

  public AdvisorNote advise(AdvisorContext context) {
    if (ai != null) {
      try {
        return ai.advise(context);
      } catch (RuntimeException e) {
        log.warn("AI advisor failed, using the rule-based advisor instead: {}", e.getMessage());
      }
    }
    return fallback.advise(context);
  }

  public boolean aiEnabled() {
    return ai != null;
  }
}
