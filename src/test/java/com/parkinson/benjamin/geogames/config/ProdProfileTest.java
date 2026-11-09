package com.parkinson.benjamin.geogames.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.parkinson.benjamin.geogames.GeogamesApplication;
import java.io.IOException;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.mock.env.MockEnvironment;

/** Nothing here connects to a database: the context fails before it creates the data source. */
class ProdProfileTest {

  @Test
  void theProdProfileReadsItsConnectionFromEnvironmentVariables() throws IOException {
    Properties prod =
        PropertiesLoaderUtils.loadProperties(new ClassPathResource("application-prod.properties"));

    assertThat(prod)
        .containsEntry("spring.datasource.url", "${DATABASE_URL}")
        .containsEntry("spring.datasource.username", "${DATABASE_USERNAME}")
        .containsEntry("spring.datasource.password", "${DATABASE_PASSWORD}")
        .containsEntry("spring.datasource.driver-class-name", "org.postgresql.Driver")
        // Logbook logs each client's address, so it must stay quiet in production.
        .containsEntry("logging.level.org.zalando.logbook", "INFO");
  }

  @Test
  void theProdProfileFailsToStartWithoutDatabaseUrl() {
    assumeTrue(System.getenv("DATABASE_URL") == null, "DATABASE_URL is set in this environment");

    assertThatThrownBy(
            () ->
                new SpringApplicationBuilder(GeogamesApplication.class)
                    .profiles("prod")
                    .web(WebApplicationType.NONE)
                    .run("--spring.main.banner-mode=off")
                    .close())
        .hasStackTraceContaining("environment variables")
        .hasStackTraceContaining("DATABASE_URL");
  }

  @Test
  void theCheckPassesWhenAllThreeVariablesAreSet() {
    MockEnvironment environment =
        new MockEnvironment()
            .withProperty("DATABASE_URL", "jdbc:postgresql://host/db")
            .withProperty("DATABASE_USERNAME", "user")
            .withProperty("DATABASE_PASSWORD", "secret");

    assertThatCode(() -> ProdDatabaseCheck.require(environment)).doesNotThrowAnyException();
  }

  @Test
  void theCheckNamesEveryMissingOrBlankVariable() {
    MockEnvironment environment =
        new MockEnvironment()
            .withProperty("DATABASE_URL", "jdbc:postgresql://host/db")
            .withProperty("DATABASE_USERNAME", "  ");

    assertThatThrownBy(() -> ProdDatabaseCheck.require(environment))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("DATABASE_USERNAME, DATABASE_PASSWORD")
        .hasMessageNotContaining("DATABASE_URL");
  }
}
