package com.studyflow.common;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Exposes the clock as a bean so services never read the system time directly. */
@Configuration
public class ClockConfig {

  @Bean
  Clock clock(@Value("${studyflow.zone}") String zone) {
    return Clock.system(ZoneId.of(zone));
  }
}
