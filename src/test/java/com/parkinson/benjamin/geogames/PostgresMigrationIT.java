package com.parkinson.benjamin.geogames;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Runs the Flyway migrations, Hibernate's schema validation and a played game against a real
 * PostgreSQL, with the {@code prod} profile on, as production does. Every other test uses H2 in
 * PostgreSQL mode, which accepts some SQL that Postgres rejects. It can't catch Neon-specific
 * settings (SSL, channel binding, the direct endpoint): see "Changing the database schema" in
 * CLAUDE.md for the manual check against a Neon branch.
 *
 * <p>Needs Docker. Without it the test is skipped on a developer machine, but never in CI (the
 * {@code CI} variable is set there), where a missing Docker must fail the build, not hide the test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("prod")
class PostgresMigrationIT {

  private static PostgreSQLContainer postgres;

  @BeforeAll
  static void startPostgres() {
    Assumptions.assumeTrue(
        System.getenv("CI") != null || DockerClientFactory.instance().isDockerAvailable(),
        "Docker is not available");
    // The major version Neon runs.
    postgres = new PostgreSQLContainer("postgres:17-alpine");
    postgres.start();
  }

  @AfterAll
  static void stopPostgres() {
    if (postgres != null) {
      postgres.stop();
    }
  }

  @DynamicPropertySource
  static void databaseProperties(DynamicPropertyRegistry registry) {
    // The same three variables the prod profile reads in Render.
    registry.add("DATABASE_URL", () -> postgres.getJdbcUrl());
    registry.add("DATABASE_USERNAME", () -> postgres.getUsername());
    registry.add("DATABASE_PASSWORD", () -> postgres.getPassword());
  }

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcClient jdbc;

  @Test
  void migrationsApplyAndAGameCanBePlayedOnPostgres() throws Exception {
    // Flyway ran every migration, and Hibernate (ddl-auto=validate) accepted the entities.
    assertThat(
            jdbc.sql("select count(*) from flyway_schema_history where success = false")
                .query(Long.class)
                .single())
        .isZero();
    assertThat(
            jdbc.sql("select count(*) from flyway_schema_history where version is not null")
                .query(Long.class)
                .single())
        .isGreaterThanOrEqualTo(2);

    String body =
        mockMvc
            .perform(
                post("/api/games")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"gameType\":\"RIVERS_BY_SHAPE\",\"numberOfRounds\":2}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String id = JsonPath.read(body, "$.id");

    mockMvc.perform(get("/api/games/%s/rounds/0".formatted(id))).andExpect(status().isOk());
    mockMvc
        .perform(
            post("/api/games/%s/rounds/0/answers".formatted(id))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"answerState\":\"CORRECT\",\"hintsUsed\":1,\"wrongGuesses\":2}"))
        .andExpect(status().isOk());

    // The stats row is written asynchronously, by JdbcClient, into the round_result table.
    await()
        .atMost(Duration.ofSeconds(10))
        .until(
            () ->
                jdbc.sql("select count(*) from round_result where answer_state = 'CORRECT'")
                        .query(Long.class)
                        .single()
                    == 1);

    // Both rounds of the game were stored.
    assertThat(jdbc.sql("select count(*) from game_round_entity").query(Long.class).single())
        .isEqualTo(2);
  }
}
