package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.dao.GameRepository;
import com.parkinson.benjamin.geogames.service.challenge.ChallengeStore;
import com.parkinson.benjamin.geogames.service.metrics.GameMetrics;
import com.parkinson.benjamin.geogames.service.metrics.GameMetrics.DeletedKind;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deletes old games to keep the database small (and, on the default in-memory H2, the heap).
 *
 * <p>It runs as soon as the app has started, then every {@code geogames.games.cleanup-interval}.
 * The free Render web service sleeps after 15 minutes without traffic, so it rarely stays up for a
 * whole interval: without the run at start-up, old games would only be deleted when the service
 * happened to stay awake for an hour.
 *
 * <p>On Neon, each hourly run wakes the database for at least 5 minutes, about 15 compute-hours a
 * month at most out of the free 100. That is acceptable, and the job doesn't run while the free web
 * service is asleep anyway. The run at start-up costs nothing extra: the service only starts
 * because a request came in, and that request uses the database too.
 *
 * <p>The delete is a single statement and is safe to run from several instances at once.
 */
@Service
public class GameCleanupService {

  private static final Logger log = LoggerFactory.getLogger(GameCleanupService.class);

  private final GameRepository games;
  private final Duration maxAge;
  private final Duration unplayedMaxAge;
  private final ChallengeStore challenges;
  private final Duration challengeMaxAge;

  private final GameMetrics metrics;

  public GameCleanupService(
      GameRepository games,
      ChallengeStore challenges,
      GameMetrics metrics,
      @Value("${geogames.games.max-age:PT24H}") Duration maxAge,
      @Value("${geogames.games.unplayed-max-age:PT2H}") Duration unplayedMaxAge,
      @Value("${geogames.challenges.max-age:P7D}") Duration challengeMaxAge) {
    this.games = games;
    this.challenges = challenges;
    this.metrics = metrics;
    this.maxAge = maxAge;
    this.unplayedMaxAge = unplayedMaxAge;
    this.challengeMaxAge = challengeMaxAge;
  }

  /** Games nobody answered a round of: a flood of these is how the live-game cap would fill. */
  public long deleteUnplayedGamesOlderThanMaxAge(Instant now) {
    long deleted = games.deleteUnplayedCreatedBefore(now.minus(unplayedMaxAge));
    metrics.deleted(DeletedKind.GAME, deleted);
    if (deleted > 0) {
      log.info("Deleted {} unplayed games older than {}", deleted, unplayedMaxAge);
    }
    return deleted;
  }

  @Transactional
  public long deleteGamesOlderThanMaxAge(Instant now) {
    long deleted = games.deleteCreatedBefore(now.minus(maxAge));
    metrics.deleted(DeletedKind.GAME, deleted);
    if (deleted > 0) {
      log.info("Deleted {} games older than {}", deleted, maxAge);
    }
    return deleted;
  }

  /** Challenges are dated, so one is deleted once the date it was last used is past the max age. */
  public int deleteChallengesOlderThanMaxAge(Instant now) {
    LocalDate cutoff = now.minus(challengeMaxAge).atZone(ZoneOffset.UTC).toLocalDate();
    int deleted = challenges.deleteUnusedBefore(cutoff);
    metrics.deleted(DeletedKind.CHALLENGE, deleted);
    if (deleted > 0) {
      log.info("Deleted {} challenges unused for {}", deleted, challengeMaxAge);
    }
    return deleted;
  }

  /**
   * Runs at start-up (no initial delay), then an interval after each run finishes. The transaction
   * comes from {@code GameRepository.deleteCreatedBefore}: the call to {@code
   * deleteGamesOlderThanMaxAge} on {@code this} does not go through the Spring proxy, so that
   * method's {@code @Transactional} does not apply here.
   */
  @Scheduled(fixedDelayString = "${geogames.games.cleanup-interval:PT1H}", initialDelay = 0)
  void deleteOldGames() {
    Instant now = Instant.now();
    deleteUnplayedGamesOlderThanMaxAge(now);
    deleteGamesOlderThanMaxAge(now);
    deleteChallengesOlderThanMaxAge(now);
  }
}
