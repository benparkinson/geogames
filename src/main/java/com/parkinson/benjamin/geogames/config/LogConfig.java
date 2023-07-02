package com.parkinson.benjamin.geogames.config;

import static org.zalando.logbook.BodyFilter.merge;
import static org.zalando.logbook.BodyFilters.defaultValue;
import static org.zalando.logbook.json.JsonPathBodyFilters.jsonPath;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.StreamingHttpOutputMessage.Body;
import org.zalando.logbook.BodyFilter;
import org.zalando.logbook.Strategy;
import org.zalando.logbook.WithoutBodyStrategy;

@Configuration
public class LogConfig {

  @Bean
  public Strategy strategy() {
    return new WithoutBodyStrategy();
  }

}
