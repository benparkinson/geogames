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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
      gameRoundEntities.add(
          new GameRoundEntity(i, gameDatum.gameType(), objectMapper.writeValueAsString(gameDatum)));
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
  public Optional<GameRound> submitAnswer(long gameId, int round, AnswerState answerState) {
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
                          r.setAnswerState(answerState);
                          gameRepository.save(game);
                          return toResponse(game, r);
                        }));
  }

  private static Optional<GameRoundEntity> findRound(GameEntity game, int roundNumber) {
    return game.getRounds().stream().filter(r -> r.getRoundNumber() == roundNumber).findFirst();
  }

  private static GameRound toResponse(GameEntity game, GameRoundEntity round) {
    return new GameRound(
        GameTypeResponse.from(round.getGameType()),
        round.getJsonBlob(),
        game.getRounds().size(),
        round.getAnswerState(),
        calculateGameResult(game));
  }

  static GameResult calculateGameResult(GameEntity game) {
    List<GameRoundEntity> rounds = game.getRounds();
    int roundsUnanswered = (int) countInState(rounds, AnswerState.UNANSWERED);
    int roundsCorrect = (int) countInState(rounds, AnswerState.CORRECT);
    GameState gameState;
    if (roundsUnanswered == 0) {
      gameState = GameState.FINISHED;
    } else if (roundsUnanswered == rounds.size()) {
      gameState = GameState.CREATED;
    } else {
      gameState = GameState.IN_PROGRESS;
    }
    List<AnswerState> roundAnswerStates =
        rounds.stream()
            .sorted(Comparator.comparingInt(GameRoundEntity::getRoundNumber))
            .map(GameRoundEntity::getAnswerState)
            .toList();
    return new GameResult(gameState, roundsUnanswered, roundsCorrect, roundAnswerStates);
  }

  private static long countInState(List<GameRoundEntity> rounds, AnswerState state) {
    return rounds.stream().filter(r -> r.getAnswerState() == state).count();
  }
}
