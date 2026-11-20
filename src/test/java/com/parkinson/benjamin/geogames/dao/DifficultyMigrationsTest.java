package com.parkinson.benjamin.geogames.dao;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/** What the difficulty migrations do to rows that already exist. */
class DifficultyMigrationsTest {

  private static Flyway flyway(DataSource dataSource, String target) {
    return Flyway.configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration")
        .target(target)
        .load();
  }

  @Test
  void oldGamesAndDailyTotalsAreBackfilled() {
    DataSource dataSource =
        new DriverManagerDataSource(
            "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
    flyway(dataSource, "12").migrate();
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    for (String hardMode : List.of("true", "false", "null")) {
      jdbc.update(
          "insert into game_entity (public_id, created_at, play_id, game_mode, hard_mode)"
              + " values (?, current_timestamp, ?, 'TRIPOINT', "
              + hardMode
              + ")",
          UUID.randomUUID(),
          UUID.randomUUID());
    }
    jdbc.update(
        "insert into daily_score (challenge_date, players, score_total, bucket_3, bucket_10)"
            + " values (date '2026-11-18', 4, 900, 3, 1)");

    flyway(dataSource, "latest").migrate();

    List<Map<String, Object>> games =
        jdbc.queryForList("select hard_mode, difficulty from game_entity");
    assertThat(games)
        .extracting(g -> g.get("HARD_MODE") + ":" + g.get("DIFFICULTY"))
        .containsExactlyInAnyOrder("true:HARD", "false:NORMAL", "null:null");
    Map<String, Object> league =
        jdbc.queryForMap(
            "select difficulty, players, score_total, bucket_3, bucket_10"
                + " from daily_score_by_difficulty where challenge_date = date '2026-11-18'");
    assertThat(league)
        .containsEntry("DIFFICULTY", "NORMAL")
        .containsEntry("PLAYERS", 4)
        .containsEntry("SCORE_TOTAL", 900)
        .containsEntry("BUCKET_3", 3)
        .containsEntry("BUCKET_10", 1);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from daily_score_by_difficulty where difficulty <> 'NORMAL'",
                Integer.class))
        .isZero();
  }
}
