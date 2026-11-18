package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.api.GameCreationResponse;
import com.parkinson.benjamin.geogames.api.GameResult;
import com.parkinson.benjamin.geogames.api.GameRound;
import com.parkinson.benjamin.geogames.api.GameTypeResponse;
import com.parkinson.benjamin.geogames.api.RecordedGuess;
import com.parkinson.benjamin.geogames.api.RoundAnswerResponse;
import com.parkinson.benjamin.geogames.api.RoundGuessesResponse;
import com.parkinson.benjamin.geogames.api.RoundHintsResponse;
import com.parkinson.benjamin.geogames.dao.GameEntity;
import com.parkinson.benjamin.geogames.dao.GameRepository;
import com.parkinson.benjamin.geogames.dao.GameRoundEntity;
import com.parkinson.benjamin.geogames.dao.RoundGuess;
import com.parkinson.benjamin.geogames.helper.PlaceNames;
import com.parkinson.benjamin.geogames.model.AnswerState;
import com.parkinson.benjamin.geogames.model.GameData;
import com.parkinson.benjamin.geogames.model.GameState;
import com.parkinson.benjamin.geogames.model.GameType;
import com.parkinson.benjamin.geogames.model.Scoring;
import com.parkinson.benjamin.geogames.service.daily.PinnedRound;
import com.parkinson.benjamin.geogames.service.stats.RoundAnswered;
import java.time.LocalDate;
import java.time.ZoneOffset;
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
import java.util.UUID;
import java.util.function.BiFunction;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class GameService {

  private static final int MAX_ROUNDS = 10;
  private static final int MAX_GUESS_LENGTH = 255;

  private final GameRepository gameRepository;
  private final Map<GameType, RoundGenerator> generators;
  private final ObjectMapper objectMapper;
  private final Random random;
  private final ApplicationEventPublisher events;
  private final KnownPlaceNames knownPlaceNames;

  public GameService(
      GameRepository gameRepository,
      List<RoundGenerator> roundGenerators,
      ObjectMapper objectMapper,
      Random random,
      ApplicationEventPublisher events,
      KnownPlaceNames knownPlaceNames) {
    this.gameRepository = gameRepository;
    this.generators = generatorsByType(roundGenerators);
    this.objectMapper = objectMapper;
    this.random = random;
    this.events = events;
    this.knownPlaceNames = knownPlaceNames;
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
          if (addGuess(r, guess, correct)) {
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
          if (addHint(r, hint, guessesBefore)) {
            gameRepository.save(game);
          }
          return new RoundHintsResponse(List.copyOf(r.getHintsTakenAt()));
        });
  }

  /** @return whether the round changed */
  private static boolean addHint(GameRoundEntity round, int hint, int guessesBefore) {
    if (round.getAnswerState() != AnswerState.UNANSWERED) {
      throw new RoundAlreadyAnsweredForGuessException(round.getRoundNumber());
    }
    int taken = round.getHintsTakenAt().size();
    if (hint < taken) {
      // Already recorded: the first request wins, so a retry changes nothing.
      return false;
    }
    if (hint != taken) {
      throw new IllegalArgumentException(
          "Hint %d can't be taken before hint %d".formatted(hint, taken));
    }
    int hintCount = round.getGameType().hintCount();
    if (hint >= hintCount) {
      throw new IllegalArgumentException(
          "A round of %s has %d hints".formatted(round.getGameType(), hintCount));
    }
    if (guessesBefore < 0) {
      throw new IllegalArgumentException("guessesBefore can't be negative");
    }
    // Clamped, not rejected: a guess whose request failed makes the client's count larger than the
    // server's, and that must not turn a real hint into an error.
    round.addHint(Math.min(guessesBefore, round.getGuesses().size()));
    return true;
  }

  /** @return whether the round changed */
  private boolean addGuess(GameRoundEntity round, String guess, boolean correct) {
    if (round.getAnswerState() != AnswerState.UNANSWERED) {
      throw new RoundAlreadyAnsweredForGuessException(round.getRoundNumber());
    }
    if (guess == null || guess.isBlank() || guess.length() > MAX_GUESS_LENGTH) {
      throw new IllegalArgumentException(
          "A guess must be between 1 and %d characters".formatted(MAX_GUESS_LENGTH));
    }
    GameType type = round.getGameType();
    String resolved =
        knownPlaceNames
            .resolve(type.placeKind(), guess)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Not a known %s name".formatted(type.placeKind().name().toLowerCase())));
    String normalised = PlaceNames.normalise(resolved);
    if (round.getGuesses().stream()
        .anyMatch(g -> PlaceNames.normalise(g.getGuess()).equals(normalised))) {
      return false;
    }
    long same = round.getGuesses().stream().filter(g -> g.isCorrect() == correct).count();
    int limit = correct ? type.answerCount() : type.guessLimit();
    if (same >= limit) {
      throw new IllegalArgumentException(
          "A round of %s allows %d %s guesses"
              .formatted(type, limit, correct ? "correct" : "wrong"));
    }
    round.addGuess(new RoundGuess(resolved, correct));
    return true;
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
    validateHintsUsed(round, hintsUsed);
    // Wrong guesses aren't checked against the stored guesses: a guess request lost on the
    // network would otherwise block a real answer.
    validateWrongGuesses(round, answerState, wrongGuesses);
    // The hints that count are at least those recorded, so a client can't report fewer. More is
    // accepted, because a hint request may have been lost. This is worked out before the repeat
    // check, so a retry is still a no-op.
    int effectiveHints = Math.max(hintsUsed, round.getHintsTakenAt().size());
    if (recordAnswer(round, answerState, effectiveHints, wrongGuesses)) {
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
          "A round of %s that ran out of guesses must have %d wrong guesses, not %d"
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
   * Concurrent answers to one round are stopped by the round's version, and the controller retries
   * once.
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
    String roundKey =
        StoredRoundKeys.withCurrentCountryNames(
            round.getRoundKey(), name -> knownPlaceNames.currentCountryName(name).orElse(name));
    GameData data = generators.get(round.getGameType()).rebuild(roundKey);
    return new GameRound(
        GameTypeResponse.from(round.getGameType()),
        objectMapper.writeValueAsString(data),
        game.getRounds().size(),
        round.getAnswerState(),
        round.getHintsUsed(),
        pointsFor(round),
        calculateGameResult(game),
        recordedGuesses(round),
        List.copyOf(round.getHintsTakenAt()));
  }

  /** The answer's response has no round data, so it needs no rebuild and no serialising. */
  private static RoundAnswerResponse toAnswerResponse(GameEntity game, GameRoundEntity round) {
    return new RoundAnswerResponse(
        round.getAnswerState(), round.getHintsUsed(), pointsFor(round), calculateGameResult(game));
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
