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
import org.springframework.transaction.annotation.Transactional;

/**
 * Challenges a friend to play the rounds of a finished game. A game has at most one challenge: the
 * one it made, or the one it was started from, and sharing it again reuses that link.
 */
@Service
public class ChallengeService {

  private final Clock clock;
  private final ChallengeStore store;
  private final ChallengeEntryStore entries;
  private final GameService gameService;
  private final GameMetrics metrics;

  public ChallengeService(
      Clock clock,
      ChallengeStore store,
      ChallengeEntryStore entries,
      GameService gameService,
      GameMetrics metrics) {
    this.clock = clock;
    this.store = store;
    this.entries = entries;
    this.gameService = gameService;
    this.metrics = metrics;
  }

  /**
   * The game's challenge, making one with this game's score as its first entry if it has none (or
   * it has expired). One transaction, so a challenge is never stored without its entry.
   *
   * @throws ChallengeNotFoundException for an unknown game
   * @throws ChallengeConflictException for an unfinished game
   * @throws BadRequestException for a daily game, which has its own share
   */
  @Transactional
  public ChallengeCreationResponse create(UUID gameId) {
    ChallengeSource source =
        gameService
            .challengeSource(gameId)
            .orElseThrow(() -> new ChallengeNotFoundException("No such game"));
    LocalDate today = LocalDate.now(clock);
    Optional<StoredChallenge> existing =
        Optional.ofNullable(source.challengeId()).flatMap(store::findById);
    if (existing.isPresent()) {
      store.touch(existing.get().id(), today);
      return new ChallengeCreationResponse(existing.get().challenge().publicId());
    }
    UUID publicId = UUID.randomUUID();
    long challengeId =
        store.insert(
            new Challenge(
                publicId,
                source.gameMode(),
                source.difficulty(),
                source.rounds(),
                source.maxScore(),
                today,
                today));
    gameService.linkChallenge(gameId, challengeId, entries.insert(challengeId, source.score()));
    metrics.challengeCreated(source.gameMode(), source.difficulty());
    return new ChallengeCreationResponse(publicId);
  }

  public ChallengeResponse find(UUID id) {
    Optional<StoredChallenge> found = store.find(id);
    metrics.challengeOpened(found.isPresent());
    StoredChallenge stored = found.orElseThrow(ChallengeService::expired);
    Challenge challenge = stored.challenge();
    return new ChallengeResponse(
        challenge.gameMode(),
        challenge.difficulty().name(),
        challenge.rounds().size(),
        entries.topScore(stored.id()),
        challenge.maxScore());
  }

  /** A new game with the challenge's rounds, mode and difficulty; each call makes another. */
  public GameCreationResponse createGame(UUID id) {
    StoredChallenge stored = store.find(id).orElseThrow(ChallengeService::expired);
    Challenge challenge = stored.challenge();
    // Touched first, so cleanup can't delete it before the new game points at it.
    store.touch(stored.id(), LocalDate.now(clock));
    GameCreationResponse game =
        gameService.createGame(
            challenge.rounds(),
            challenge.gameMode(),
            challenge.difficulty(),
            null,
            entries.topScore(stored.id()),
            stored.id());
    metrics.challengeStarted(challenge.gameMode(), challenge.difficulty());
    return game;
  }

  private static ChallengeNotFoundException expired() {
    return new ChallengeNotFoundException("This challenge has expired");
  }
}
