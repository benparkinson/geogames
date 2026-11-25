package com.parkinson.benjamin.geogames.service.stats;

import com.parkinson.benjamin.geogames.model.Difficulty;
import com.parkinson.benjamin.geogames.model.Scoring;
import com.parkinson.benjamin.geogames.model.stats.RoundResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Records each answered round once its answer has committed, on the {@code statsExecutor} thread,
 * so the answer's request never waits for, or fails because of, the statistics. A failure only
 * drops the row. Off when {@code geogames.stats.enabled} is false.
 */
@Component
@ConditionalOnProperty(name = "geogames.stats.enabled", havingValue = "true")
public class RoundStatsRecorder {

  private static final Logger log = LoggerFactory.getLogger(RoundStatsRecorder.class);

  private final RoundStatsStore store;

  public RoundStatsRecorder(RoundStatsStore store) {
    this.store = store;
  }

  @Async("statsExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void record(RoundAnswered event) {
    try {
      // The rules the round was played under: an Easy round has its own hint count and guess limit.
      Difficulty played = Difficulty.orNormal(event.difficulty());
      int hintCount = event.gameType().hintCount(played);
      store.insert(
          new RoundResult(
              event.answeredOn(),
              event.playId(),
              event.gameMode(),
              event.roundsInGame(),
              event.roundNumber(),
              event.gameType().name(),
              event.roundKey(),
              event.answerState().name(),
              event.hintsUsed(),
              event.wrongGuesses(),
              hintCount,
              event.gameType().guessLimit(played),
              Scoring.roundPoints(
                  event.answerState(),
                  event.hintsUsed(),
                  event.answersFound() == null ? 0 : event.answersFound(),
                  event.gameType().answerCount(),
                  event.wrongGuesses() == null ? 0 : event.wrongGuesses()),
              event.hardMode(),
              event.difficulty()));
    } catch (Exception e) {
      // The message only: never the play id, and a stack trace adds nothing for a dropped row.
      log.warn("Dropped a round statistics row: {}", e.toString());
    }
  }
}
