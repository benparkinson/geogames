package com.parkinson.benjamin.geogames.service.daily;

import com.parkinson.benjamin.geogames.model.Difficulty;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Running totals of each day's finished daily challenges: one row per date and difficulty (a
 * league), no per-player rows. A game of unknown difficulty counts as NORMAL.
 *
 * <p>{@link #ensureDay} is deliberately not {@code @Transactional}, for the same reason as {@link
 * DailyChallengeStore}: on Postgres a failed insert aborts its transaction. {@link
 * #recordFinished} is called inside {@code GameService}'s transaction, and {@link JdbcClient} on
 * the same data source joins it, so a rolled-back answer rolls the count back too.
 */
@Component
public class DailyScoreStore {

  private static final Logger log = LoggerFactory.getLogger(DailyScoreStore.class);

  private final JdbcClient jdbc;

  public DailyScoreStore(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * Makes sure the league's row for the day exists, so it is there before any game for the date can
   * finish.
   */
  public void ensureDay(LocalDate date, Difficulty difficulty) {
    boolean exists =
        jdbc.sql(
                    "select count(*) from daily_score_by_difficulty"
                        + " where challenge_date = :date and difficulty = :difficulty")
                .param("date", date)
                .param("difficulty", league(difficulty))
                .query(Integer.class)
                .single()
            > 0;
    if (exists) {
      return;
    }
    try {
      jdbc.sql(
              "insert into daily_score_by_difficulty (challenge_date, difficulty)"
                  + " values (:date, :difficulty)")
          .param("date", date)
          .param("difficulty", league(difficulty))
          .update();
    } catch (DuplicateKeyException lostTheRace) {
      // Another instance made the row first.
    }
  }

  /**
   * Adds one finished game with this score to the day's totals. A missing row is logged (the date
   * only) and ignored: a count must never fail an answer.
   */
  public void recordFinished(LocalDate date, Difficulty difficulty, int score, int maxScore) {
    int bucket = bucket(score, maxScore);
    // The bucket is an int from 0 to 10 computed above, never text from outside, so it is safe to
    // put in the column name.
    int updated =
        jdbc.sql(
                "update daily_score_by_difficulty set players = players + 1,"
                    + " score_total = score_total + :score,"
                    + " bucket_"
                    + bucket
                    + " = bucket_"
                    + bucket
                    + " + 1 where challenge_date = :date and difficulty = :difficulty")
            .param("score", score)
            .param("date", date)
            .param("difficulty", league(difficulty))
            .update();
    if (updated == 0) {
      log.warn(
          "No daily score row for {} ({}); a finished game was not counted",
          date,
          league(difficulty));
    }
  }

  /** The league's totals for the day, or zeros when there is no row. */
  public DailyScore find(LocalDate date, Difficulty difficulty) {
    Optional<DailyScore> row =
        jdbc.sql(
                "select players, score_total, bucket_0, bucket_1, bucket_2, bucket_3, bucket_4,"
                    + " bucket_5, bucket_6, bucket_7, bucket_8, bucket_9, bucket_10"
                    + " from daily_score_by_difficulty"
                    + " where challenge_date = :date and difficulty = :difficulty")
            .param("date", date)
            .param("difficulty", league(difficulty))
            .query(
                (rs, rowNum) -> {
                  List<Integer> buckets = new ArrayList<>(DailyScore.BUCKETS);
                  for (int i = 0; i < DailyScore.BUCKETS; i++) {
                    buckets.add(rs.getInt(3 + i));
                  }
                  return new DailyScore(rs.getInt(1), rs.getInt(2), List.copyOf(buckets));
                })
            .optional();
    return row.orElseGet(DailyScore::empty);
  }

  /** The league a game counts in: unknown (null) is NORMAL. */
  private static String league(Difficulty difficulty) {
    return Difficulty.orNormal(difficulty).name();
  }

  /** The 10% band of the maximum a score is in; 10 is a perfect score. */
  static int bucket(int score, int maxScore) {
    if (maxScore <= 0 || score < 0 || score > maxScore) {
      throw new IllegalArgumentException("Score %d is not within 0..%d".formatted(score, maxScore));
    }
    return Math.min(10, score * 10 / maxScore);
  }
}
