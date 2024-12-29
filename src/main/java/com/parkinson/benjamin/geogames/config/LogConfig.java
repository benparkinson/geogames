package com.parkinson.benjamin.geogames.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zalando.logbook.Strategy;
import org.zalando.logbook.core.WithoutBodyStrategy;

@Configuration
public class LogConfig {

  @Bean
  public Strategy strategy() {
    return new WithoutBodyStrategy();
  }
}
