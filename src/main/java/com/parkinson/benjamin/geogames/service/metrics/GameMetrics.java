package com.parkinson.benjamin.geogames.service.metrics;

import com.parkinson.benjamin.geogames.model.Difficulty;
import com.parkinson.benjamin.geogames.service.stats.RoundAnswered;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Counts games started and rounds answered for the metrics export. Tags are enum values only (the
 * mode, game type, difficulty, answer state and whether a hint was used), never an id, a round key
 * or anything a player typed. The counters are in-process and reset on restart.
 */
@Component
public class GameMetrics {

  private static final String UNKNOWN = "UNKNOWN";

  private final MeterRegistry registry;

  public GameMetrics(MeterRegistry registry) {
    this.registry = registry;
  }

  /** {@code gameMode} is {@code RANDOM}, {@code DAILY} or a game type name. */
  public void gameStarted(String gameMode, Difficulty difficulty) {
    Counter.builder("geogames.games.started")
        .tag("mode", gameMode)
        .tag("difficulty", difficultyName(difficulty))
        .register(registry)
        .increment();
  }

  /** Never log the event: its {@code toString} prints the play id. */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onRoundAnswered(RoundAnswered event) {
    String gameType = event.gameType().name();
    String difficulty = difficultyName(event.difficulty());
    Counter.builder("geogames.rounds.answered")
        .tag("game_type", gameType)
        .tag("difficulty", difficulty)
        .tag("answer_state", event.answerState().name())
        .tag("hinted", Boolean.toString(event.hintsUsed() > 0))
        .register(registry)
        .increment();
    count("geogames.rounds.hints", gameType, difficulty, event.hintsUsed());
    if (event.wrongGuesses() != null) {
      count("geogames.rounds.wrong_guesses", gameType, difficulty, event.wrongGuesses());
      count("geogames.rounds.guess_reported", gameType, difficulty, 1);
    }
  }

  private void count(String name, String gameType, String difficulty, int amount) {
    Counter.builder(name)
        .tag("game_type", gameType)
        .tag("difficulty", difficulty)
        .register(registry)
        .increment(amount);
  }

  private static String difficultyName(Difficulty difficulty) {
    return difficulty == null ? UNKNOWN : difficulty.name();
  }
}
