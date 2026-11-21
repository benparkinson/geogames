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

  /** Every subject used on any of the days. */
  Set<String> subjects() {
    Set<String> all = new HashSet<>();
    days.forEach(day -> all.addAll(day.subjects()));
    return all;
  }

  /** Days since the type was last used, or {@code absent} if no day in the history used it. */
  int daysSinceUsed(GameType type, int absent) {
    return days.stream()
        .filter(day -> day.types().contains(type))
        .mapToInt(PastDay::daysAgo)
        .min()
        .orElse(absent);
  }
}
