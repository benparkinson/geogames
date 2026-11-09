package com.parkinson.benjamin.geogames.config;

import java.util.List;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

/**
 * Stops the {@code prod} profile from starting without its database settings, with a message that
 * names the missing variables. Without this, Spring leaves the unresolved {@code ${DATABASE_URL}}
 * in place and the failure is a confusing "driver claims to not accept jdbcUrl".
 */
@Configuration(proxyBeanMethods = false)
@Profile("prod")
class ProdDatabaseCheck {

  private static final List<String> REQUIRED =
      List.of("DATABASE_URL", "DATABASE_USERNAME", "DATABASE_PASSWORD");

  /** A bean factory post-processor runs before any other bean, so nothing connects first. */
  @Bean
  static BeanFactoryPostProcessor requireDatabaseSettings(Environment environment) {
    return beanFactory -> require(environment);
  }

  static void require(Environment environment) {
    List<String> missing =
        REQUIRED.stream().filter(name -> isBlank(environment.getProperty(name))).toList();
    if (!missing.isEmpty()) {
      throw new IllegalStateException(
          "The prod profile needs these environment variables, but they are not set: "
              + String.join(", ", missing));
    }
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
