package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.api.GameCreationResponse;
import com.parkinson.benjamin.geogames.api.GameResult;
import com.parkinson.benjamin.geogames.api.GameRound;
import com.parkinson.benjamin.geogames.api.GameTypeResponse;
import com.parkinson.benjamin.geogames.dao.GameEntity;
import com.parkinson.benjamin.geogames.dao.GameRepository;
import com.parkinson.benjamin.geogames.dao.GameRoundEntity;
import com.parkinson.benjamin.geogames.model.AnswerState;
import com.parkinson.benjamin.geogames.model.GameData;
import com.parkinson.benjamin.geogames.model.GameState;
import com.parkinson.benjamin.geogames.model.GameType;
import com.parkinson.benjamin.geogames.model.Scoring;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class GameService {

  private static final int MAX_ROUNDS = 10;

  private final GameRepository gameRepository;
  private final Map<GameType, RoundGenerator> generators;
  private final ObjectMapper objectMapper;
  private final Random random;

  public GameService(
      GameRepository gameRepository,
      List<RoundGenerator> roundGenerators,
      ObjectMapper objectMapper,
      Random random) {
    this.gameRepository = gameRepository;
    this.generators = generatorsByType(roundGenerators);
    this.objectMapper = objectMapper;
    this.random = random;
  }

  private static Map<GameType, RoundGenerator> generatorsByType(List<RoundGenerator> generators) {
    Map<GameType, RoundGenerator> byType = new EnumMap<>(GameType.class);
    for (RoundGenerator generator : generators) {
      if (byType.put(generator.gameType(), generator) != null) {
        throw new IllegalStateException(
            "More than one round generator for game type " + generator.gameType());
      }
    }
    for (GameType type : GameType.values()) {
      if (!byType.containsKey(type)) {
        throw new IllegalStateException("No round generator for game type " + type);
      }
    }
    return byType;
  }

  @Transactional
  public GameCreationResponse createGame(String gameType, int numberOfRounds) {
    if (numberOfRounds < 1 || numberOfRounds > MAX_ROUNDS) {
      throw new IllegalArgumentException(
          "Number of rounds must be between 1 and %d, was %d!"
              .formatted(MAX_ROUNDS, numberOfRounds));
    }

    GameSelection selection = GameSelection.parse(gameType);
    List<GameData> gameData =
        selection.isRandom()
            ? generateMixedRounds(numberOfRounds)
            : generators.get(selection.type()).generate(numberOfRounds, Set.of());

    GameEntity saved = gameRepository.save(new GameEntity(createGameRounds(gameData)));

    return new GameCreationResponse(saved.getId());
  }

  /** Gives each round a random game type, then shuffles so the types come in a random order. */
  private List<GameData> generateMixedRounds(int numberOfRounds) {
    GameType[] types = GameType.values();
    Map<GameType, Integer> roundsPerType = new EnumMap<>(GameType.class);
    for (int i = 0; i < numberOfRounds; i++) {
      roundsPerType.merge(types[random.nextInt(types.length)], 1, Integer::sum);
    }

    List<GameData> allGameData = new ArrayList<>(numberOfRounds);
    Set<String> used = new HashSet<>();
    roundsPerType.forEach(
        (type, count) -> {
          List<GameData> generated = generators.get(type).generate(count, Set.copyOf(used));
          generated.forEach(data -> used.add(data.subjectId()));
          allGameData.addAll(generated);
        });
    Collections.shuffle(allGameData, random);
    return allGameData;
  }

  private List<GameRoundEntity> createGameRounds(List<GameData> gameData) {
    List<GameRoundEntity> gameRoundEntities = new ArrayList<>(gameData.size());
    for (int i = 0; i < gameData.size(); i++) {
      GameData gameDatum = gameData.get(i);
      gameRoundEntities.add(new GameRoundEntity(i, gameDatum.gameType(), gameDatum.roundKey()));
    }
    return gameRoundEntities;
  }

  @Transactional(readOnly = true)
  public Optional<GameRound> getGameRound(long gameId, int round) {
    return gameRepository
        .findById(gameId)
        .flatMap(game -> findRound(game, round).map(r -> toResponse(game, r)));
  }

  @Transactional
  public Optional<GameRound> submitAnswer(
      long gameId, int round, AnswerState answerState, int hintsUsed, Integer wrongGuesses) {
    if (answerState == AnswerState.UNANSWERED) {
      throw new IllegalArgumentException("A round can't be answered with " + answerState);
    }

    return gameRepository
        .findById(gameId)
        .flatMap(
            game ->
                findRound(game, round)
                    .map(
                        r -> {
                          validateHintsUsed(r, hintsUsed);
                          validateWrongGuesses(r, answerState, wrongGuesses);
                          if (recordAnswer(r, answerState, hintsUsed, wrongGuesses)) {
                            gameRepository.save(game);
                          }
                          return toResponse(game, r);
                        }));
  }

  private static void validateHintsUsed(GameRoundEntity round, int hintsUsed) {
    int hintCount = round.getGameType().hintCount();
    if (hintsUsed < 0 || hintsUsed > hintCount) {
      throw new IllegalArgumentException(
          "A round of %s has %d hints, so %d can't have been used"
              .formatted(round.getGameType(), hintCount, hintsUsed));
    }
  }

  /**
   * Checks the reported wrong guesses against the guess limit: running out of guesses means exactly
   * {@code guessLimit} wrong guesses, and any other answer means fewer. A null (not reported) is
   * always accepted.
   */
  private static void validateWrongGuesses(
      GameRoundEntity round, AnswerState answerState, Integer wrongGuesses) {
    if (wrongGuesses == null) {
      return;
    }
    GameType type = round.getGameType();
    int limit = type.guessLimit();
    if (wrongGuesses < 0 || wrongGuesses > limit) {
      throw new IllegalArgumentException(
          "A round of %s allows %d wrong guesses, so %d is not possible"
              .formatted(type, limit, wrongGuesses));
    }
    if (answerState == AnswerState.OUT_OF_GUESSES && wrongGuesses != limit) {
      throw new IllegalArgumentException(
          "A round of %s that ran out of guesses had %d wrong guesses, not %d"
              .formatted(type, limit, wrongGuesses));
    }
    if (answerState != AnswerState.OUT_OF_GUESSES && wrongGuesses == limit) {
      throw new IllegalArgumentException(
          "A round of %s with %d wrong guesses ran out of guesses, so it can't be %s"
              .formatted(type, wrongGuesses, answerState));
    }
  }

  /**
   * A round's answer is final. Sending the same answer (and hints and wrong guesses) again does
   * nothing, so a retried request is harmless, but changing an answered round is a conflict.
   *
   * @return whether the round changed
   */
  private static boolean recordAnswer(
      GameRoundEntity round, AnswerState answerState, int hintsUsed, Integer wrongGuesses) {
    AnswerState current = round.getAnswerState();
    if (current != AnswerState.UNANSWERED) {
      if (current == answerState
          && round.getHintsUsed() == hintsUsed
          && Objects.equals(round.getWrongGuesses(), wrongGuesses)) {
        return false;
      }
      throw new RoundAlreadyAnsweredException(
          round.getRoundNumber(),
          current,
          round.getHintsUsed(),
          round.getWrongGuesses(),
          answerState,
          hintsUsed,
          wrongGuesses);
    }
    round.setAnswerState(answerState);
    round.setHintsUsed(hintsUsed);
    round.setWrongGuesses(wrongGuesses);
    return true;
  }

  private static Optional<GameRoundEntity> findRound(GameEntity game, int roundNumber) {
    return game.getRounds().stream().filter(r -> r.getRoundNumber() == roundNumber).findFirst();
  }

  private GameRound toResponse(GameEntity game, GameRoundEntity round) {
    GameData data = generators.get(round.getGameType()).rebuild(round.getRoundKey());
    return new GameRound(
        GameTypeResponse.from(round.getGameType()),
        objectMapper.writeValueAsString(data),
        game.getRounds().size(),
        round.getAnswerState(),
        round.getHintsUsed(),
        pointsFor(round),
        calculateGameResult(game));
  }

  private static int pointsFor(GameRoundEntity round) {
    return Scoring.roundPoints(
        round.getAnswerState(), round.getHintsUsed(), round.getGameType().hintCount());
  }

  static GameResult calculateGameResult(GameEntity game) {
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
      int points = pointsFor(round);
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
