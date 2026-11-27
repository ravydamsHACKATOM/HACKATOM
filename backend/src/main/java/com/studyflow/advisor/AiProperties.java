package com.studyflow.advisor;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings of the optional AI advisor. The key only comes from the environment, never from the
 * repository.
 */
@ConfigurationProperties("studyflow.ai")
public record AiProperties(String apiKey, String model, String baseUrl, int timeoutSeconds) {

  public boolean enabled() {
    return apiKey != null && !apiKey.isBlank();
  }
}
