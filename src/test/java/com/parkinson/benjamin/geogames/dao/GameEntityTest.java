package com.parkinson.benjamin.geogames.dao;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkinson.benjamin.geogames.model.Difficulty;
import com.parkinson.benjamin.geogames.model.GameOrigin;
import com.parkinson.benjamin.geogames.model.GameType;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameEntityTest {

  private static GameEntity game(LocalDate dailyDate, Integer challengeScore) {
    return new GameEntity(
        List.of(new GameRoundEntity(0, GameType.TRIPOINT, "tripoint:a")),
        "TRIPOINT",
        Difficulty.NORMAL,
        dailyDate,
        challengeScore);
  }

  @Test
  void theOriginComesFromTheDailyDateAndTheChallengeScore() {
    assertThat(game(null, null).origin()).isEqualTo(GameOrigin.FREE_PLAY);
    assertThat(game(LocalDate.parse("2026-11-26"), null).origin()).isEqualTo(GameOrigin.DAILY);
    // A score of 0 still has to be beaten.
    assertThat(game(null, 0).origin()).isEqualTo(GameOrigin.CHALLENGE);
  }
}
