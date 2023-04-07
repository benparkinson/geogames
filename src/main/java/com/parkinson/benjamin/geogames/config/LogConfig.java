package com.parkinson.benjamin.geogames.config;

import static org.zalando.logbook.BodyFilter.merge;
import static org.zalando.logbook.BodyFilters.defaultValue;
import static org.zalando.logbook.json.JsonPathBodyFilters.jsonPath;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zalando.logbook.BodyFilter;

@Configuration
public class LogConfig {

  @Bean
  public BodyFilter bodyFilter() {
    return merge(
        defaultValue(),
        jsonPath("$..coordinates").delete());
  }
}
