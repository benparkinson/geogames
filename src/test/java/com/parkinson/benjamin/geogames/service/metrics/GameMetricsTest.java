package com.parkinson.benjamin.geogames.service.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkinson.benjamin.geogames.model.AnswerState;
import com.parkinson.benjamin.geogames.model.Difficulty;
import com.parkinson.benjamin.geogames.model.GameOrigin;
import com.parkinson.benjamin.geogames.model.GameType;
import com.parkinson.benjamin.geogames.service.metrics.GameMetrics.DeletedKind;
import com.parkinson.benjamin.geogames.service.metrics.GameMetrics.DropReason;
import com.parkinson.benjamin.geogames.service.metrics.GameMetrics.InitialsResult;
import com.parkinson.benjamin.geogames.service.metrics.GameMetrics.RejectReason;
import com.parkinson.benjamin.geogames.service.stats.GameFinished;
import com.parkinson.benjamin.geogames.service.stats.RoundAnswered;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class GameMetricsTest {

  private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
  private final GameMetrics metrics = new GameMetrics(registry);

  @Test
  void countsGamesStartedByModeAndDifficulty() {
    metrics.gameStarted("DAILY", Difficulty.HARD, GameOrigin.DAILY);
    metrics.gameStarted("DAILY", Difficulty.HARD, GameOrigin.DAILY);
    metrics.gameStarted("RANDOM", null, GameOrigin.FREE_PLAY);

    assertThat(started("DAILY", "HARD")).isEqualTo(2);
    assertThat(started("RANDOM", "UNKNOWN")).isEqualTo(1);
    assertThat(value("geogames.games.started", "mode", "RANDOM", "origin", "FREE_PLAY"))
        .isEqualTo(1);
  }

  @Test
  void countsAnAnsweredRoundWithItsHintsAndWrongGuesses() {
    metrics.onRoundAnswered(event(AnswerState.CORRECT, 2, 3, Difficulty.EASY));
    metrics.onRoundAnswered(event(AnswerState.GAVE_UP, 0, 1, Difficulty.EASY));

    assertThat(answered("CORRECT", "true")).isEqualTo(1);
    assertThat(answered("GAVE_UP", "false")).isEqualTo(1);
    assertThat(count("geogames.rounds.hints")).isEqualTo(2);
    assertThat(count("geogames.rounds.wrong_guesses")).isEqualTo(4);
    assertThat(count("geogames.rounds.guess_reported")).isEqualTo(2);
  }

  @Test
  void anUnknownWrongGuessCountIsNotCounted() {
    metrics.onRoundAnswered(event(AnswerState.CORRECT, 0, null, null));

    assertThat(count("geogames.rounds.answered")).isEqualTo(1);
    assertThat(count("geogames.rounds.wrong_guesses")).isZero();
    assertThat(count("geogames.rounds.guess_reported")).isZero();
  }

  @ParameterizedTest
  @CsvSource({"0,50,0", "4,50,0", "5,50,1", "49,50,9", "50,50,10", "100,100,10", "60,50,10"})
  void aFinishedGameIsCountedInItsScoreBand(int score, int maxScore, String band) {
    metrics.onGameFinished(
        new GameFinished(GameOrigin.DAILY, Difficulty.EASY, score, maxScore, null, null));

    assertThat(
            value(
                "geogames.games.finished",
                "origin",
                "DAILY",
                "difficulty",
                "EASY",
                "score_band",
                band))
        .isEqualTo(1);
    assertThat(count("geogames.games.finished")).isEqualTo(1);
    assertThat(count("geogames.challenges.finished")).isZero();
  }

  @ParameterizedTest
  @CsvSource({"31,BEAT", "30,TIED", "29,LOST"})
  void aFinishedChallengeGameIsCountedByItsOutcome(int score, String outcome) {
    metrics.onGameFinished(new GameFinished(GameOrigin.CHALLENGE, null, score, 50, 30, null));

    assertThat(value("geogames.challenges.finished", "difficulty", "UNKNOWN", "outcome", outcome))
        .isEqualTo(1);
    assertThat(count("geogames.challenges.finished")).isEqualTo(1);
  }

  @ParameterizedTest
  @CsvSource({"1,1", "2,2", "3,3-5", "5,3-5", "6,6-10", "10,6-10", "11,11+"})
  void aChallengeEntryIsCountedInItsTableSizeBand(int tableSize, String band) {
    metrics.challengeEntryAdded(tableSize);

    assertThat(value("geogames.challenges.entries", "table_size", band)).isEqualTo(1);
    assertThat(count("geogames.challenges.entries")).isEqualTo(1);
  }

  @ParameterizedTest
  @CsvSource({"true", "false"})
  void aFinishedChallengeEntryIsCountedWithWhetherItMadeTheTop(boolean top5) {
    metrics.onGameFinished(
        new GameFinished(
            GameOrigin.CHALLENGE, null, 40, 50, 30, new GameFinished.ChallengeEntry(top5, 3)));

    assertThat(value("geogames.challenges.finished", "top5", Boolean.toString(top5))).isEqualTo(1);
    assertThat(count("geogames.challenges.finished")).isEqualTo(1);
    assertThat(value("geogames.challenges.entries", "table_size", "3-5")).isEqualTo(1);
  }

  @ParameterizedTest
  @EnumSource(InitialsResult.class)
  void countsInitialsByResult(InitialsResult result) {
    metrics.challengeInitials(result);

    assertThat(value("geogames.challenges.initials", "result", result.name())).isEqualTo(1);
    assertThat(count("geogames.challenges.initials")).isEqualTo(1);
  }

  @Test
  void countsChallengesByModeDifficultyAndResult() {
    metrics.challengeCreated("RANDOM", Difficulty.HARD);
    metrics.challengeStarted("TRIPOINT", Difficulty.EASY);
    metrics.challengeOpened(true);
    metrics.challengeOpened(false);

    assertThat(value("geogames.challenges.created", "mode", "RANDOM", "difficulty", "HARD"))
        .isEqualTo(1);
    assertThat(value("geogames.challenges.started", "mode", "TRIPOINT", "difficulty", "EASY"))
        .isEqualTo(1);
    assertThat(value("geogames.challenges.opened", "result", "FOUND")).isEqualTo(1);
    assertThat(value("geogames.challenges.opened", "result", "EXPIRED")).isEqualTo(1);
  }

  @Test
  void countsRefusedGamesDroppedRowsAndDeletions() {
    metrics.gameRejected(RejectReason.CAPACITY);
    metrics.statsDropped(DropReason.QUEUE_FULL);
    metrics.deleted(DeletedKind.GAME, 3);

    assertThat(value("geogames.games.rejected", "reason", "CAPACITY")).isEqualTo(1);
    assertThat(value("geogames.games.rejected", "reason", "RATE_LIMITED")).isZero();
    assertThat(value("geogames.games.rejected", "reason", "GLOBAL_RATE")).isZero();
    assertThat(value("geogames.stats.dropped", "reason", "QUEUE_FULL")).isEqualTo(1);
    assertThat(value("geogames.cleanup.deleted", "kind", "GAME")).isEqualTo(3);
    assertThat(value("geogames.cleanup.deleted", "kind", "CHALLENGE")).isZero();
  }

  @Test
  void everySeriesExistsAtZeroFromTheStartAndCountingAddsNone() {
    int registered = registry.getMeters().size();
    assertThat(registered).isLessThan(1500);
    assertThat(
            value(
                "geogames.games.finished",
                "origin",
                "CHALLENGE",
                "difficulty",
                "HARD",
                "score_band",
                "10"))
        .isZero();

    for (GameType type : GameType.values()) {
      metrics.gameStarted(type.name(), null, GameOrigin.CHALLENGE);
      metrics.challengeCreated(type.name(), Difficulty.NORMAL);
    }
    metrics.onRoundAnswered(event(AnswerState.OUT_OF_GUESSES, 1, 1, null));
    metrics.onGameFinished(new GameFinished(GameOrigin.FREE_PLAY, null, 7, 10, null, null));
    metrics.onGameFinished(
        new GameFinished(
            GameOrigin.CHALLENGE, null, 7, 10, 5, new GameFinished.ChallengeEntry(true, 11)));
    metrics.challengeInitials(InitialsResult.BLOCKED);

    assertThat(registry.getMeters()).hasSize(registered);
  }

  private double value(String name, String... tags) {
    return registry.get(name).tags(tags).counters().stream().mapToDouble(c -> c.count()).sum();
  }

  private double started(String mode, String difficulty) {
    return value("geogames.games.started", "mode", mode, "difficulty", difficulty);
  }

  private double answered(String state, String hinted) {
    return value("geogames.rounds.answered", "answer_state", state, "hinted", hinted);
  }

  private double count(String name) {
    return registry.find(name).counters().stream().mapToDouble(c -> c.count()).sum();
  }

  private static RoundAnswered event(
      AnswerState state, int hints, Integer wrongGuesses, Difficulty difficulty) {
    return new RoundAnswered(
        UUID.randomUUID(),
        "RANDOM",
        3,
        0,
        GameType.TRIPOINT,
        "{}",
        state,
        hints,
        wrongGuesses,
        null,
        LocalDate.of(2026, 1, 1),
        null,
        difficulty);
  }
}
