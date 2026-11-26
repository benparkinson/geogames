package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.api.BadRequestException;
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
import com.parkinson.benjamin.geogames.model.AnswerState;
import com.parkinson.benjamin.geogames.model.Difficulty;
import com.parkinson.benjamin.geogames.model.GameData;
import com.parkinson.benjamin.geogames.model.GameState;
import com.parkinson.benjamin.geogames.service.challenge.ChallengeConflictException;
import com.parkinson.benjamin.geogames.service.daily.DailyScoreStore;
import com.parkinson.benjamin.geogames.service.daily.PinnedRound;
import com.parkinson.benjamin.geogames.service.metrics.GameMetrics;
import com.parkinson.benjamin.geogames.service.stats.RoundAnswered;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
  private final RoundGenerators generators;
  private final ObjectMapper objectMapper;
  private final Random random;
  private final ApplicationEventPublisher events;
  private final RoundRules rules;
  private final KnownPlaceNames placeNames;
  private final DailyScoreStore dailyScores;
  private final RoundContinents roundContinents;
  private final GameMetrics metrics;

  public GameService(
      GameRepository gameRepository,
      RoundGenerators generators,
      RoundRules rules,
      ObjectMapper objectMapper,
      Random random,
      ApplicationEventPublisher events,
      KnownPlaceNames placeNames,
      DailyScoreStore dailyScores,
      RoundContinents roundContinents,
      GameMetrics metrics) {
    this.gameRepository = gameRepository;
    this.generators = generators;
    this.objectMapper = objectMapper;
    this.random = random;
    this.events = events;
    this.rules = rules;
    this.placeNames = placeNames;
    this.dailyScores = dailyScores;
    this.roundContinents = roundContinents;
    this.metrics = metrics;
  }

  @Transactional
  public GameCreationResponse createGame(
      String gameType, int numberOfRounds, Difficulty difficulty) {
    if (numberOfRounds < 1 || numberOfRounds > MAX_ROUNDS) {
      throw new BadRequestException(
          "Number of rounds must be between 1 and %d, was %d!"
              .formatted(MAX_ROUNDS, numberOfRounds));
    }

    GameSelection selection = GameSelection.parse(gameType);
    List<GameData> gameData =
        selection.isRandom()
            ? generators.generateMixed(numberOfRounds, random)
            : generators.generate(selection.type(), numberOfRounds, Set.of(), random);

    return createGame(
        gameData.stream().map(PinnedRound::of).toList(),
        selection.isRandom() ? "RANDOM" : selection.type().name(),
        difficulty,
        null);
  }

  /** Makes a game from rounds that were chosen earlier (a daily challenge's pinned rounds). */
  @Transactional
  public GameCreationResponse createGame(
      List<PinnedRound> rounds, String gameMode, Difficulty difficulty, LocalDate dailyDate) {
    return createGame(rounds, gameMode, difficulty, dailyDate, null);
  }

  /** The same, with the score to beat for a game started from a challenge. */
  @Transactional
  public GameCreationResponse createGame(
      List<PinnedRound> rounds,
      String gameMode,
      Difficulty difficulty,
      LocalDate dailyDate,
      Integer challengeScore) {
    List<GameRoundEntity> entities = new ArrayList<>(rounds.size());
    for (int i = 0; i < rounds.size(); i++) {
      entities.add(new GameRoundEntity(i, rounds.get(i).gameType(), rounds.get(i).roundKey()));
    }
    GameCreationResponse response =
        new GameCreationResponse(
            gameRepository
                .save(new GameEntity(entities, gameMode, difficulty, dailyDate, challengeScore))
                .getPublicId());
    metrics.gameStarted(gameMode, difficulty);
    return response;
  }

  /**
   * What a challenge copies from a game, or empty if there is no such game.
   *
   * @throws BadRequestException for a daily game
   * @throws ChallengeConflictException if a round is unanswered
   */
  @Transactional(readOnly = true)
  public Optional<ChallengeSource> challengeSource(UUID gameId) {
    return gameRepository
        .findByPublicId(gameId)
        .map(
            game -> {
              if (game.getDailyDate() != null) {
                throw new BadRequestException("A daily game has its own share");
              }
              GameResult result = GameResults.of(game);
              if (result.gameState() != GameState.FINISHED) {
                throw new ChallengeConflictException("Finish the game to challenge a friend");
              }
              List<PinnedRound> rounds =
                  game.getRounds().stream()
                      .sorted(Comparator.comparingInt(GameRoundEntity::getRoundNumber))
                      .map(r -> new PinnedRound(r.getGameType(), r.getRoundKey()))
                      .toList();
              return new ChallengeSource(
                  rounds, game.getGameMode(), game.difficulty(), result.score(), result.maxScore());
            });
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
          if (rules.addGuess(r, game.difficulty(), guess, correct)) {
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
          if (rules.addHint(r, game.difficulty(), hint, guessesBefore)) {
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
      UUID gameId,
      int round,
      AnswerState answerState,
      int hintsUsed,
      Integer wrongGuesses,
      Integer answersFound) {
    if (answerState == AnswerState.UNANSWERED) {
      throw new BadRequestException("A round can't be answered with " + answerState);
    }

    return withRound(
        gameId,
        round,
        (game, r) -> answer(game, r, answerState, hintsUsed, wrongGuesses, answersFound));
  }

  private RoundAnswerResponse answer(
      GameEntity game,
      GameRoundEntity round,
      AnswerState answerState,
      int hintsUsed,
      Integer wrongGuesses,
      Integer answersFound) {
    // Wrong guesses aren't checked against the stored guesses: a guess request lost on the
    // network would otherwise block a real answer.
    rules.validateAnswer(
        round, game.difficulty(), answerState, hintsUsed, wrongGuesses, answersFound);
    // The hints that count are at least those recorded, so a client can't report fewer. More is
    // accepted, because a hint request may have been lost. This is worked out before the repeat
    // check, so a retry is still a no-op.
    int effectiveHints = Math.max(hintsUsed, round.getHintsTakenAt().size());
    Integer effectiveFound = effectiveAnswersFound(round, answerState, answersFound);
    if (rules.recordAnswer(round, answerState, effectiveHints, wrongGuesses, effectiveFound)) {
      gameRepository.save(game);
      publishRoundAnswered(game, round);
      // Counted here, in this transaction, so a rolled-back answer rolls the count back too. This
      // is the first answer to the round, so a repeat never gets here.
      if (game.getDailyDate() != null && allAnswered(game)) {
        GameResult result = GameResults.of(game);
        dailyScores.recordFinished(
            game.getDailyDate(), game.difficulty(), result.score(), result.maxScore());
      }
    }
    return toAnswerResponse(game, round);
  }

  /**
   * The answers found that count: all of them for a correct round, otherwise the larger of what
   * the client reported and the correct guesses recorded (a guess request may have been lost, and
   * the client checks answers anyway), below the answer count. Null if the client didn't say and no
   * correct guess was recorded. Worked out before the repeat check, so a retry is still a no-op.
   */
  private static Integer effectiveAnswersFound(
      GameRoundEntity round, AnswerState answerState, Integer reported) {
    int answerCount = round.getGameType().answerCount();
    if (answerState == AnswerState.CORRECT) {
      return answerCount;
    }
    int recorded = (int) round.getGuesses().stream().filter(g -> g.isCorrect()).count();
    if (reported == null && recorded == 0) {
      return null;
    }
    return Math.min(Math.max(reported == null ? 0 : reported, recorded), answerCount - 1);
  }

  private static boolean allAnswered(GameEntity game) {
    return game.getRounds().stream().noneMatch(r -> r.getAnswerState() == AnswerState.UNANSWERED);
  }

  /**
   * Tells the usage statistics a round was answered. The event is handled after this transaction
   * commits.
   */
  private void publishRoundAnswered(GameEntity game, GameRoundEntity round) {
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
            round.getAnswersFound(),
            LocalDate.now(ZoneOffset.UTC),
            game.getDifficulty() == null ? null : game.getDifficulty() == Difficulty.HARD,
            game.getDifficulty()));
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
    String roundKey = placeNames.currentRoundKey(round.getRoundKey());
    GameData data = generators.rebuild(round.getGameType(), roundKey);
    return new GameRound(
        GameTypeResponse.from(round.getGameType(), game.difficulty()),
        objectMapper.writeValueAsString(data),
        game.getRounds().size(),
        round.getAnswerState(),
        round.getHintsUsed(),
        GameResults.pointsFor(round, game.difficulty()),
        GameResults.of(game),
        recordedGuesses(round),
        List.copyOf(round.getHintsTakenAt()),
        game.difficulty().name(),
        game.difficulty() == Difficulty.EASY ? roundContinents.of(data) : List.of());
  }

  /** The answer's response has no round data, so it needs no rebuild and no serialising. */
  private static RoundAnswerResponse toAnswerResponse(GameEntity game, GameRoundEntity round) {
    return new RoundAnswerResponse(
        round.getAnswerState(),
        round.getHintsUsed(),
        GameResults.pointsFor(round, game.difficulty()),
        GameResults.of(game));
  }
}
