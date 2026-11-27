package com.parkinson.benjamin.geogames.service.challenge;

import com.parkinson.benjamin.geogames.api.BadRequestException;
import com.parkinson.benjamin.geogames.api.ChallengeCreationResponse;
import com.parkinson.benjamin.geogames.api.ChallengeResponse;
import com.parkinson.benjamin.geogames.api.GameCreationResponse;
import com.parkinson.benjamin.geogames.service.ChallengeSource;
import com.parkinson.benjamin.geogames.service.GameService;
import com.parkinson.benjamin.geogames.service.metrics.GameMetrics;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Challenges a friend to play the rounds of a finished game. Not {@code @Transactional}: the store
 * commits its own insert and {@link GameService} makes the game in its own transaction.
 */
@Service
public class ChallengeService {

  private final Clock clock;
  private final ChallengeStore store;
  private final GameService gameService;

  private final GameMetrics metrics;

  public ChallengeService(
      Clock clock, ChallengeStore store, GameService gameService, GameMetrics metrics) {
    this.clock = clock;
    this.store = store;
    this.gameService = gameService;
    this.metrics = metrics;
  }

  /**
   * @throws ChallengeNotFoundException for an unknown game
   * @throws ChallengeConflictException for an unfinished game
   * @throws BadRequestException for a daily game, which has its own share
   */
  public ChallengeCreationResponse create(UUID gameId) {
    ChallengeSource source =
        gameService
            .challengeSource(gameId)
            .orElseThrow(() -> new ChallengeNotFoundException("No such game"));
    UUID id = UUID.randomUUID();
    store.insert(
        new Challenge(
            id,
            source.gameMode(),
            source.difficulty(),
            source.rounds(),
            source.score(),
            source.maxScore(),
            LocalDate.now(clock)));
    metrics.challengeCreated(source.gameMode(), source.difficulty());
    return new ChallengeCreationResponse(id);
  }

  public ChallengeResponse find(UUID id) {
    Optional<Challenge> found = store.find(id);
    metrics.challengeOpened(found.isPresent());
    Challenge challenge = found.orElseThrow(ChallengeService::expired);
    return new ChallengeResponse(
        challenge.gameMode(),
        challenge.difficulty().name(),
        challenge.rounds().size(),
        challenge.score(),
        challenge.maxScore());
  }

  /** A new game with the challenge's rounds, mode and difficulty; each call makes another. */
  public GameCreationResponse createGame(UUID id) {
    Challenge challenge = load(id);
    GameCreationResponse game =
        gameService.createGame(
            challenge.rounds(),
            challenge.gameMode(),
            challenge.difficulty(),
            null,
            challenge.score());
    metrics.challengeStarted(challenge.gameMode(), challenge.difficulty());
    return game;
  }

  private Challenge load(UUID id) {
    return store.find(id).orElseThrow(ChallengeService::expired);
  }

  private static ChallengeNotFoundException expired() {
    return new ChallengeNotFoundException("This challenge has expired");
  }
}
