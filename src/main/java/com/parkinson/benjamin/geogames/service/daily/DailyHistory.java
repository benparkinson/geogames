package com.parkinson.benjamin.geogames.service.daily;

import com.parkinson.benjamin.geogames.model.Continent;
import com.parkinson.benjamin.geogames.model.GameType;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The earlier days, nearest first. {@link #EMPTY} for Daily #1, for tests, and when nothing is
 * stored.
 */
record DailyHistory(List<PastDay> days) {

  static final DailyHistory EMPTY = new DailyHistory(List.of());

  /** What the picker needs to know about one earlier day. */
  record PastDay(
      int daysAgo,
      Set<GameType> types,
      Set<String> subjects,
      List<Set<Continent>> roundContinents) {}

  /** Every subject used on the days up to {@code maxDaysAgo} days back. */
  Set<String> subjects(int maxDaysAgo) {
    Set<String> all = new HashSet<>();
    days.stream()
        .filter(day -> day.daysAgo() <= maxDaysAgo)
        .forEach(day -> all.addAll(day.subjects()));
    return all;
  }

  /**
   * Days since the type was last used within the last {@code window} days, or {@code window + 1}
   * if it wasn't.
   */
  int daysSinceUsed(GameType type, int window) {
    return days.stream()
        .filter(day -> day.daysAgo() <= window && day.types().contains(type))
        .mapToInt(PastDay::daysAgo)
        .min()
        .orElse(window + 1);
  }

  /** How many of the last {@code window} days used the type. */
  int recentUses(GameType type, int window) {
    return (int)
        days.stream().filter(day -> day.daysAgo() <= window && day.types().contains(type)).count();
  }
}
