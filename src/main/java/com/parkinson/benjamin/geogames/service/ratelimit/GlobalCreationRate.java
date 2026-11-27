package com.parkinson.benjamin.geogames.service.ratelimit;

import java.time.Clock;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A fixed one-minute window on how many games the whole server creates, whoever asks. It stops a
 * flood from many addresses filling the live-game cap: the site recovers within a minute of the
 * flood stopping. Holds one window (an end time and a count), nothing per client.
 */
public class GlobalCreationRate {

  private static final long WINDOW_MILLIS = 60_000;

  private record Window(long endMillis, int count) {}

  private final Clock clock;
  private final int perMinute;
  private final AtomicReference<Window> window = new AtomicReference<>(new Window(0, 0));

  public GlobalCreationRate(Clock clock, int perMinute) {
    this.clock = clock;
    this.perMinute = perMinute;
  }

  /** Empty when the creation is allowed (and counted), else the seconds left in the minute. */
  public OptionalLong tryAcquire() {
    long now = clock.millis();
    long[] wait = {-1};
    window.updateAndGet(
        current -> {
          Window w = now >= current.endMillis() ? new Window(now + WINDOW_MILLIS, 0) : current;
          if (w.count() >= perMinute) {
            wait[0] = Math.max(1, (w.endMillis() - now + 999) / 1000);
            return w;
          }
          wait[0] = -1;
          return new Window(w.endMillis(), w.count() + 1);
        });
    return wait[0] < 0 ? OptionalLong.empty() : OptionalLong.of(wait[0]);
  }
}
