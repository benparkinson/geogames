package com.parkinson.benjamin.geogames.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

  /** UTC, so "today" on the server doesn't depend on the host's time zone. Tests fix a clock. */
  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }
}
