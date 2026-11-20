package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.api.GameResult;
import com.parkinson.benjamin.geogames.dao.GameEntity;
import com.parkinson.benjamin.geogames.dao.GameRoundEntity;
import com.parkinson.benjamin.geogames.model.AnswerState;
import com.parkinson.benjamin.geogames.model.Difficulty;
import com.parkinson.benjamin.geogames.model.GameState;
import com.parkinson.benjamin.geogames.model.Scoring;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Works out the points of a round and the overall result of a game from the stored answers. */
final class GameResults {

  private GameResults() {}

  static int pointsFor(GameRoundEntity round, Difficulty difficulty) {
    Integer found = round.getAnswersFound();
    return Scoring.roundPoints(
        round.getAnswerState(),
        round.getHintsUsed(),
        round.getGameType().hintCount(difficulty),
        found == null ? 0 : found,
        round.getGameType().answerCount());
  }

  static GameResult of(GameEntity game) {
    List<GameRoundEntity> rounds =
        game.getRounds().stream()
            .sorted(Comparator.comparingInt(GameRoundEntity::getRoundNumber))
            .toList();
    List<AnswerState> roundAnswerStates = new ArrayList<>(rounds.size());
    List<Integer> roundPoints = new ArrayList<>(rounds.size());
    List<Integer> roundHintsUsed = new ArrayList<>(rounds.size());
    int roundsUnanswered = 0;
    int roundsCorrect = 0;
    int perfectRounds = 0;
    int score = 0;
    for (GameRoundEntity round : rounds) {
      AnswerState state = round.getAnswerState();
      int points = pointsFor(round, game.difficulty());
      roundAnswerStates.add(state);
      roundPoints.add(points);
      roundHintsUsed.add(round.getHintsUsed());
      score += points;
      if (state == AnswerState.UNANSWERED) {
        roundsUnanswered++;
      } else if (state == AnswerState.CORRECT) {
        roundsCorrect++;
        if (round.getHintsUsed() == 0) {
          perfectRounds++;
        }
      }
    }
    GameState gameState;
    if (roundsUnanswered == 0) {
      gameState = GameState.FINISHED;
    } else if (roundsUnanswered == rounds.size()) {
      gameState = GameState.CREATED;
    } else {
      gameState = GameState.IN_PROGRESS;
    }
    return new GameResult(
        gameState,
        roundsUnanswered,
        score,
        roundAnswerStates,
        Scoring.MAX_ROUND_POINTS * rounds.size(),
        roundsCorrect,
        perfectRounds,
        roundPoints,
        roundHintsUsed);
  }
}
