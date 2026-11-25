package com.parkinson.benjamin.geogames.service.ratelimit;

import com.parkinson.benjamin.geogames.dao.GameRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Duration;
import java.util.OptionalLong;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Called first by both game-creation endpoints: the per-client limit, then the global creation rate, then the live-game cap. */
@Component
public class GameCreationGuard {

  static final long CAPACITY_RETRY_SECONDS = 600;

  private final ClientRateLimiter limiter;
  private final GlobalCreationRate globalRate;
  private final LiveGameCap cap;
  private final int forwardedHops;

  public GameCreationGuard(
      GameRepository repository,
      Clock clock,
      @Value("${geogames.rate-limit.games-per-window:0}") int gamesPerWindow,
      @Value("${geogames.rate-limit.window:PT10M}") Duration window,
      @Value("${geogames.rate-limit.max-clients:10000}") int maxClients,
      @Value("${geogames.rate-limit.forwarded-hops:1}") int forwardedHops,
      @Value("${geogames.rate-limit.global-games-per-minute:0}") int globalPerMinute,
      @Value("${geogames.games.max-live:50000}") long maxLive) {
    this.limiter =
        gamesPerWindow > 0
            ? new ClientRateLimiter(clock, gamesPerWindow, window, maxClients)
            : null;
    this.globalRate = globalPerMinute > 0 ? new GlobalCreationRate(clock, globalPerMinute) : null;
    this.cap = new LiveGameCap(repository, clock, maxLive);
    this.forwardedHops = forwardedHops;
  }

  public void check(HttpServletRequest request) {
    if (limiter != null) {
      OptionalLong wait = limiter.tryAcquire(ClientAddress.key(request, forwardedHops));
      if (wait.isPresent()) {
        throw new TooManyGamesException(wait.getAsLong());
      }
    }
    if (globalRate != null) {
      OptionalLong wait = globalRate.tryAcquire();
      if (wait.isPresent()) {
        throw new GameCapacityException(wait.getAsLong());
      }
    }
    if (cap.isFull()) {
      throw new GameCapacityException(CAPACITY_RETRY_SECONDS);
    }
  }
}
