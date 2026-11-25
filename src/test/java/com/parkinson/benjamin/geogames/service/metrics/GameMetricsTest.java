package com.parkinson.benjamin.geogames.service.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkinson.benjamin.geogames.model.AnswerState;
import com.parkinson.benjamin.geogames.model.Difficulty;
import com.parkinson.benjamin.geogames.model.GameType;
import com.parkinson.benjamin.geogames.service.stats.RoundAnswered;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GameMetricsTest {

  private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
  private final GameMetrics metrics = new GameMetrics(registry);

  @Test
  void countsGamesStartedByModeAndDifficulty() {
    metrics.gameStarted("DAILY", Difficulty.HARD);
    metrics.gameStarted("DAILY", Difficulty.HARD);
    metrics.gameStarted("RANDOM", null);

    assertThat(started("DAILY", "HARD")).isEqualTo(2);
    assertThat(started("RANDOM", "UNKNOWN")).isEqualTo(1);
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

    assertThat(registry.find("geogames.rounds.answered").tag("difficulty", "UNKNOWN").counter())
        .isNotNull();
    assertThat(registry.find("geogames.rounds.wrong_guesses").counter()).isNull();
    assertThat(registry.find("geogames.rounds.guess_reported").counter()).isNull();
  }

  private double started(String mode, String difficulty) {
    return registry
        .get("geogames.games.started")
        .tag("mode", mode)
        .tag("difficulty", difficulty)
        .counter()
        .count();
  }

  private double answered(String state, String hinted) {
    return registry
        .get("geogames.rounds.answered")
        .tag("answer_state", state)
        .tag("hinted", hinted)
        .counter()
        .count();
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
