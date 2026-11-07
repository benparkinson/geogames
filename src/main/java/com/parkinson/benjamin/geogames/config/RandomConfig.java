package com.parkinson.benjamin.geogames.config;

import java.util.Random;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RandomConfig {

  /** The one source of randomness for picking rounds. Tests pass a seeded one instead. */
  @Bean
  public Random random() {
    return new Random();
  }
}
