package com.parkinson.benjamin.geogames.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.parkinson.benjamin.geogames.dao.GameEntity;
import com.parkinson.benjamin.geogames.dao.GameRoundEntity;
import com.parkinson.benjamin.geogames.dao.GameType;
import com.parkinson.benjamin.geogames.model.AnswerState;
import com.parkinson.benjamin.geogames.model.GameResult;
import com.parkinson.benjamin.geogames.model.GameState;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GameServiceTest {

  private static GameRoundEntity round(int index, AnswerState state) {
    GameRoundEntity entity = new GameRoundEntity(index, GameType.TRIPOINT, "{}");
    entity.setAnswerState(state);
    return entity;
  }

  private static GameEntity game(GameRoundEntity... rounds) {
    return new GameEntity(new ArrayList<>(List.of(rounds)));
  }

  @Test
  void roundAnswerStatesAreOrderedByIndexAndScored() {
    GameEntity game =
        game(
            round(2, AnswerState.UNANSWERED),
            round(0, AnswerState.CORRECT),
            round(1, AnswerState.GAVE_UP));

    GameResult result = GameService.calculateGameResult(Optional.of(game));

    assertEquals(
        List.of(AnswerState.CORRECT, AnswerState.GAVE_UP, AnswerState.UNANSWERED),
        result.roundAnswerStates());
    assertEquals(1, result.score());
    assertEquals(1, result.roundsUnanswered());
    assertEquals(GameState.IN_PROGRESS, result.gameState());
  }

  @Test
  void gameIsFinishedWhenEveryRoundIsAnswered() {
    GameEntity game = game(round(1, AnswerState.GAVE_UP), round(0, AnswerState.CORRECT));

    GameResult result = GameService.calculateGameResult(Optional.of(game));

    assertEquals(GameState.FINISHED, result.gameState());
    assertEquals(0, result.roundsUnanswered());
    assertEquals(List.of(AnswerState.CORRECT, AnswerState.GAVE_UP), result.roundAnswerStates());
  }
}
