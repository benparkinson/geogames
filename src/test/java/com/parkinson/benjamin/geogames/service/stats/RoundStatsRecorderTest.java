package com.parkinson.benjamin.geogames.service.stats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.parkinson.benjamin.geogames.model.AnswerState;
import com.parkinson.benjamin.geogames.model.GameType;
import com.parkinson.benjamin.geogames.model.stats.RoundResult;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RoundStatsRecorderTest {

  private final RoundStatsStore store = mock(RoundStatsStore.class);
  private final RoundStatsRecorder recorder = new RoundStatsRecorder(store);

  private static RoundAnswered event(Integer wrongGuesses) {
    return new RoundAnswered(
        UUID.randomUUID(),
        "RANDOM",
        4,
        2,
        GameType.RIVERS_BY_SHAPE,
        "river:Q1",
        AnswerState.CORRECT,
        1,
        wrongGuesses,
        LocalDate.of(2026, 11, 9));
  }

  private RoundResult recorded() {
    ArgumentCaptor<RoundResult> captor = ArgumentCaptor.forClass(RoundResult.class);
    verify(store).insert(captor.capture());
    return captor.getValue();
  }

  @Test
  void aCorrectRoundBecomesARowWithThePointsAndTheTypesSettings() {
    RoundAnswered event = event(2);

    recorder.record(event);

    RoundResult row = recorded();
    GameType type = GameType.RIVERS_BY_SHAPE;
    assertThat(row)
        .isEqualTo(
            new RoundResult(
                event.answeredOn(),
                event.playId(),
                "RANDOM",
                4,
                2,
                "RIVERS_BY_SHAPE",
                "river:Q1",
                "CORRECT",
                1,
                2,
                type.hintCount(),
                type.guessLimit(),
                com.parkinson.benjamin.geogames.model.Scoring.roundPoints(
                    AnswerState.CORRECT, 1, type.hintCount())));
    assertThat(row.points()).isBetween(1, 99);
  }

  @Test
  void anUnreportedWrongGuessCountStaysNull() {
    recorder.record(event(null));

    assertThat(recorded().wrongGuesses()).isNull();
  }

  @Test
  void aFailingStoreDoesNotPropagate() {
    doThrow(new IllegalStateException("db down")).when(store).insert(any());

    assertThatCode(() -> recorder.record(event(0))).doesNotThrowAnyException();
  }
}
