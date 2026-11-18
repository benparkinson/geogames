package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.api.GameCreationResponse;
import com.parkinson.benjamin.geogames.api.GameRound;
import com.parkinson.benjamin.geogames.api.GameTypeResponse;
import com.parkinson.benjamin.geogames.api.RecordedGuess;
import com.parkinson.benjamin.geogames.api.RoundAnswerResponse;
import com.parkinson.benjamin.geogames.api.RoundGuessesResponse;
import com.parkinson.benjamin.geogames.api.RoundHintsResponse;
import com.parkinson.benjamin.geogames.dao.GameEntity;
import com.parkinson.benjamin.geogames.dao.GameRepository;
import com.parkinson.benjamin.geogames.dao.GameRoundEntity;
import com.parkinson.benjamin.geogames.model.AnswerState;
import com.parkinson.benjamin.geogames.model.GameData;
import com.parkinson.benjamin.geogames.model.GameType;
import com.parkinson.benjamin.geogames.service.daily.PinnedRound;
import com.parkinson.benjamin.geogames.service.stats.RoundAnswered;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import org.springframework.context.ApplicationEventPublisher;
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
  private final ApplicationEventPublisher events;
  private final RoundRules rules;

  public GameService(
      GameRepository gameRepository,
      List<RoundGenerator> roundGenerators,
      RoundRules rules,
      ObjectMapper objectMapper,
      Random random,
      ApplicationEventPublisher events) {
    this.gameRepository = gameRepository;
    this.generators = generatorsByType(roundGenerators);
    this.objectMapper = objectMapper;
    this.random = random;
    this.events = events;
    this.rules = rules;
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
            : generators.get(selection.type()).generate(numberOfRounds, Set.of(), random);

    return createGame(
        gameData.stream().map(PinnedRound::of).toList(),
        selection.isRandom() ? "RANDOM" : selection.type().name());
  }

  /** Makes a game from rounds that were chosen earlier (a daily challenge's pinned rounds). */
  @Transactional
  public GameCreationResponse createGame(List<PinnedRound> rounds, String gameMode) {
    List<GameRoundEntity> entities = new ArrayList<>(rounds.size());
    for (int i = 0; i < rounds.size(); i++) {
      entities.add(new GameRoundEntity(i, rounds.get(i).gameType(), rounds.get(i).roundKey()));
    }
    return new GameCreationResponse(
        gameRepository.save(new GameEntity(entities, gameMode)).getPublicId());
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
          List<GameData> generated = generators.get(type).generate(count, Set.copyOf(used), random);
          generated.forEach(data -> used.add(data.subjectId()));
          allGameData.addAll(generated);
        });
    Collections.shuffle(allGameData, random);
    return allGameData;
  }

  @Transactional(readOnly = true)
  public Optional<GameRound> getGameRound(UUID gameId, int round) {
    return withRound(gameId, round, this::toResponse);
  }

  /**
   * Stores a guess the client recorded, as the list's spelling of a known place name. The server
   * doesn't check {@code correct}: the client checks answers. It publishes no statistics event,
   * which are recorded once per answered round.
   *
   * @return empty if the game or round doesn't exist
   */
  @Transactional
  public Optional<RoundGuessesResponse> recordGuess(
      UUID gameId, int round, String guess, boolean correct) {
    return withRound(
        gameId,
        round,
        (game, r) -> {
          if (rules.addGuess(r, guess, correct)) {
            gameRepository.save(game);
          }
          return new RoundGuessesResponse(recordedGuesses(r));
        });
  }

  /**
   * Stores that hint {@code hint} was taken after {@code guessesBefore} guesses. Like guesses, it
   * publishes no statistics event.
   *
   * @return empty if the game or round doesn't exist
   */
  @Transactional
  public Optional<RoundHintsResponse> recordHint(
      UUID gameId, int round, int hint, int guessesBefore) {
    return withRound(
        gameId,
        round,
        (game, r) -> {
          if (rules.addHint(r, hint, guessesBefore)) {
            gameRepository.save(game);
          }
          return new RoundHintsResponse(List.copyOf(r.getHintsTakenAt()));
        });
  }

  private static List<RecordedGuess> recordedGuesses(GameRoundEntity round) {
    return round.getGuesses().stream()
        .map(g -> new RecordedGuess(g.getGuess(), g.isCorrect()))
        .toList();
  }

  @Transactional
  public Optional<RoundAnswerResponse> submitAnswer(
      UUID gameId, int round, AnswerState answerState, int hintsUsed, Integer wrongGuesses) {
    if (answerState == AnswerState.UNANSWERED) {
      throw new IllegalArgumentException("A round can't be answered with " + answerState);
    }

    return withRound(
        gameId, round, (game, r) -> answer(game, r, answerState, hintsUsed, wrongGuesses));
  }

  private RoundAnswerResponse answer(
      GameEntity game,
      GameRoundEntity round,
      AnswerState answerState,
      int hintsUsed,
      Integer wrongGuesses) {
    // Wrong guesses aren't checked against the stored guesses: a guess request lost on the
    // network would otherwise block a real answer.
    rules.validateAnswer(round, answerState, hintsUsed, wrongGuesses);
    // The hints that count are at least those recorded, so a client can't report fewer. More is
    // accepted, because a hint request may have been lost. This is worked out before the repeat
    // check, so a retry is still a no-op.
    int effectiveHints = Math.max(hintsUsed, round.getHintsTakenAt().size());
    if (rules.recordAnswer(round, answerState, effectiveHints, wrongGuesses)) {
      gameRepository.save(game);
      publishRoundAnswered(game, round);
    }
    return toAnswerResponse(game, round);
  }

  /**
   * Tells the usage statistics a round was answered. The event is handled after this transaction
   * commits. A game made before the statistics existed has no play id and is skipped.
   */
  private void publishRoundAnswered(GameEntity game, GameRoundEntity round) {
    if (game.getPlayId() == null) {
      return;
    }
    events.publishEvent(
        new RoundAnswered(
            game.getPlayId(),
            game.getGameMode(),
            game.getRounds().size(),
            round.getRoundNumber(),
            round.getGameType(),
            round.getRoundKey(),
            round.getAnswerState(),
            round.getHintsUsed(),
            round.getWrongGuesses(),
            LocalDate.now(ZoneOffset.UTC)));
  }

  private <T> Optional<T> withRound(
      UUID gameId, int round, BiFunction<GameEntity, GameRoundEntity, T> action) {
    return gameRepository
        .findByPublicId(gameId)
        .flatMap(game -> findRound(game, round).map(r -> action.apply(game, r)));
  }

  private static Optional<GameRoundEntity> findRound(GameEntity game, int roundNumber) {
    return game.getRounds().stream().filter(r -> r.getRoundNumber() == roundNumber).findFirst();
  }

  private GameRound toResponse(GameEntity game, GameRoundEntity round) {
    String roundKey = rules.currentRoundKey(round.getRoundKey());
    GameData data = generators.get(round.getGameType()).rebuild(roundKey);
    return new GameRound(
        GameTypeResponse.from(round.getGameType()),
        objectMapper.writeValueAsString(data),
        game.getRounds().size(),
        round.getAnswerState(),
        round.getHintsUsed(),
        GameResults.pointsFor(round),
        GameResults.of(game),
        recordedGuesses(round),
        List.copyOf(round.getHintsTakenAt()));
  }

  /** The answer's response has no round data, so it needs no rebuild and no serialising. */
  private static RoundAnswerResponse toAnswerResponse(GameEntity game, GameRoundEntity round) {
    return new RoundAnswerResponse(
        round.getAnswerState(),
        round.getHintsUsed(),
        GameResults.pointsFor(round),
        GameResults.of(game));
  }
}
