package com.parkinson.benjamin.geogames.service.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Clock;
import java.time.Duration;
import java.util.OptionalLong;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Limits every API request per client, so one script can't tie up the CPU or the database pool. It
 * does not save database compute time: any request now and then keeps the database awake. It has
 * its own table and salt, separate from the game-creation limit. CORS preflights aren't counted.
 */
@Component
public class RequestRateLimitInterceptor implements HandlerInterceptor {

  private final ClientRateLimiter limiter;
  private final int forwardedHops;

  /** The clock is optional so that web-layer test slices, which have no Clock bean, still start. */
  @Autowired
  public RequestRateLimitInterceptor(
      ObjectProvider<Clock> clock,
      @Value("${geogames.rate-limit.requests-per-window:0}") int requestsPerWindow,
      @Value("${geogames.rate-limit.request-window:PT1M}") Duration window,
      @Value("${geogames.rate-limit.max-clients:10000}") int maxClients,
      @Value("${geogames.rate-limit.forwarded-hops:1}") int forwardedHops) {
    this(
        clock.getIfAvailable(Clock::systemUTC),
        requestsPerWindow,
        window,
        maxClients,
        forwardedHops);
  }

  RequestRateLimitInterceptor(
      Clock clock, int requestsPerWindow, Duration window, int maxClients, int forwardedHops) {
    this.limiter =
        requestsPerWindow > 0
            ? new ClientRateLimiter(clock, requestsPerWindow, window, maxClients)
            : null;
    this.forwardedHops = forwardedHops;
  }

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    if (limiter == null || "OPTIONS".equals(request.getMethod())) {
      return true;
    }
    OptionalLong wait = limiter.tryAcquire(ClientAddress.key(request, forwardedHops));
    if (wait.isPresent()) {
      throw new TooManyRequestsException(wait.getAsLong());
    }
    return true;
  }
}
