package com.parkinson.benjamin.geogames.service.ratelimit;

import com.parkinson.benjamin.geogames.dao.GameRepository;
import java.time.Clock;
import java.util.concurrent.atomic.AtomicReference;

/** Refuses new games while the database holds the maximum; the count is cached for 60 seconds. */
public class LiveGameCap {

  static final long CACHE_MILLIS = 60_000;

  private record Count(long value, long atMillis) {}

  private final GameRepository repository;
  private final Clock clock;
  private final long max;
  private final AtomicReference<Count> cached = new AtomicReference<>();

  public LiveGameCap(GameRepository repository, Clock clock, long max) {
    this.repository = repository;
    this.clock = clock;
    this.max = max;
  }

  public boolean isFull() {
    long now = clock.millis();
    Count count = cached.get();
    if (count == null || now - count.atMillis() >= CACHE_MILLIS) {
      count = new Count(repository.count(), now);
      cached.set(count);
    }
    return count.value() >= max;
  }
}
