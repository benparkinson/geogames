package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.dao.GameRepository;
import java.time.Duration;
import java.time.Instant;
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
 */
@Service
public class GameCleanupService {

  private static final Logger log = LoggerFactory.getLogger(GameCleanupService.class);

  private final GameRepository games;
  private final Duration maxAge;

  public GameCleanupService(
      GameRepository games, @Value("${geogames.games.max-age:PT24H}") Duration maxAge) {
    this.games = games;
    this.maxAge = maxAge;
  }

  @Transactional
  public long deleteGamesOlderThanMaxAge(Instant now) {
    long deleted = games.deleteByCreatedAtBefore(now.minus(maxAge));
    if (deleted > 0) {
      log.info("Deleted {} games older than {}", deleted, maxAge);
    }
    return deleted;
  }

  /** Runs at start-up (no initial delay), then an interval after each run finishes. */
  @Scheduled(fixedDelayString = "${geogames.games.cleanup-interval:PT1H}", initialDelay = 0)
  void deleteOldGames() {
    deleteGamesOlderThanMaxAge(Instant.now());
  }
}
