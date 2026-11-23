package com.parkinson.benjamin.geogames.service.ratelimit;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A fixed-window limit per client, held in memory. The map is keyed by a salted hash of the address
 * (the salt is made at start-up and never saved), so the address itself is never stored, logged or
 * put in a message. When the map is full, expired windows are dropped; if it is still full the
 * request is allowed (failing open, so many addresses can't lock everyone out).
 */
public class ClientRateLimiter {

  private record Window(long endMillis, int count) {}

  private final Clock clock;
  private final int limit;
  private final long windowMillis;
  private final int maxClients;
  private final byte[] salt = new byte[16];
  private final Map<String, Window> windows = new ConcurrentHashMap<>();

  public ClientRateLimiter(Clock clock, int limit, Duration window, int maxClients) {
    this.clock = clock;
    this.limit = limit;
    this.windowMillis = window.toMillis();
    this.maxClients = maxClients;
    new SecureRandom().nextBytes(salt);
  }

  /** Empty when the request is allowed (and counted), otherwise the seconds until it may be. */
  public OptionalLong tryAcquire(String address) {
    String key = key(address);
    long now = clock.millis();
    synchronized (windows) {
      Window window = windows.get(key);
      if (window == null && windows.size() >= maxClients) {
        windows.values().removeIf(w -> w.endMillis() <= now);
        if (windows.size() >= maxClients) {
          return OptionalLong.empty();
        }
      }
      if (window == null || window.endMillis() <= now) {
        windows.put(key, new Window(now + windowMillis, 1));
        return OptionalLong.empty();
      }
      if (window.count() >= limit) {
        return OptionalLong.of(Math.max(1, (window.endMillis() - now + 999) / 1000));
      }
      windows.put(key, new Window(window.endMillis(), window.count() + 1));
      return OptionalLong.empty();
    }
  }

  /** For tests: the keys held. */
  Set<String> keys() {
    return windows.keySet();
  }

  private String key(String address) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      digest.update(salt);
      digest.update(address.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(Arrays.copyOf(digest.digest(), 16));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is always available", e);
    }
  }
}
