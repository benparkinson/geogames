package com.parkinson.benjamin.geogames.service.daily;

import com.parkinson.benjamin.geogames.model.Continent;
import com.parkinson.benjamin.geogames.model.GameData;
import com.parkinson.benjamin.geogames.model.GameType;
import com.parkinson.benjamin.geogames.service.RoundContinents;
import com.parkinson.benjamin.geogames.service.RoundGenerators;
import com.parkinson.benjamin.geogames.service.daily.DailyHistory.PastDay;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Chooses the five rounds of a day's challenge from a seed made of the date and the last few days:
 * five different game types (never the extra difficult ones), each about a different subject. The
 * same date and history give the same rounds for the same code and data; {@link
 * DailyChallengeStore} pins them so a deploy can't change a day that has started.
 *
 * <p>The history comes from {@link DailyHistoryLoader} (the stored days, rebuilt, not stored
 * again). Types are ranked by days since they were last used, so a type that sat out yesterday
 * plays today and none sits out two days running. Subjects used in the history are excluded.
 * Continents are best effort: up to {@value #CONTINENT_ATTEMPTS} attempts at the rounds, keeping
 * the first with no {@link #continentPenalty} or else the lowest.
 */
@Service
public class DailyRoundPicker {

  public static final int DAILY_ROUNDS = 5;

  static final int CONTINENT_ATTEMPTS = 8;

  /** Days of history the continent score compares with. */
  private static final int CONTINENT_DAYS = 2;

  private final RoundGenerators generators;
  private final RoundContinents continents;
  private final List<GameType> pool;

  public DailyRoundPicker(RoundGenerators generators, RoundContinents continents) {
    this.generators = generators;
    this.continents = continents;
    this.pool = Arrays.stream(GameType.values()).filter(type -> !type.extraDifficult()).toList();
    if (pool.size() < DAILY_ROUNDS) {
      throw new IllegalStateException(
          "The daily challenge needs %d game types that aren't extra difficult, found %d"
              .formatted(DAILY_ROUNDS, pool.size()));
    }
  }

  public List<GameData> pick(LocalDate date, DailyHistory history) {
    Random random = new Random(seedFor(date));
    List<GameType> types = chooseTypes(history, random);
    Set<String> recentSubjects = history.subjects();

    List<GameData> best = null;
    int bestPenalty = Integer.MAX_VALUE;
    for (int attempt = 0; attempt < CONTINENT_ATTEMPTS; attempt++) {
      List<GameData> rounds = generateRounds(types, recentSubjects, random);
      int penalty = penaltyOf(rounds, history);
      if (penalty == 0) {
        return rounds;
      }
      if (penalty < bestPenalty) {
        best = rounds;
        bestPenalty = penalty;
      }
    }
    return best;
  }

  /** The {@link #continentPenalty} of a day's rounds. */
  int penaltyOf(List<GameData> rounds, DailyHistory history) {
    return continentPenalty(
        rounds.stream().map(round -> continentsOf(continents, round)).toList(), history);
  }

  private List<GameType> chooseTypes(DailyHistory history, Random random) {
    int absent = DailyHistoryLoader.LOOKBACK_DAYS + 1;
    List<GameType> types = new ArrayList<>(pool);
    Collections.shuffle(types, random);
    // The sort is stable, so the shuffle breaks ties.
    types.sort(
        Comparator.comparingInt((GameType type) -> history.daysSinceUsed(type, absent)).reversed());
    List<GameType> chosen = new ArrayList<>(types.subList(0, DAILY_ROUNDS));
    Collections.shuffle(chosen, random);
    return chosen;
  }

  private List<GameData> generateRounds(
      List<GameType> types, Set<String> recentSubjects, Random random) {
    List<GameData> rounds = new ArrayList<>(DAILY_ROUNDS);
    Set<String> today = new HashSet<>();
    for (GameType type : types) {
      Set<String> excluded = new HashSet<>(recentSubjects);
      excluded.addAll(today);
      GameData round;
      try {
        round = generators.generate(type, 1, excluded, random).get(0);
      } catch (IllegalArgumentException tooFewLeft) {
        // Never expected with the real data: drop the history's exclusions, keep today's.
        round = generators.generate(type, 1, Set.copyOf(today), random).get(0);
      }
      today.add(round.subjectId());
      rounds.add(round);
    }
    return rounds;
  }

  /** The round's continents; none if they can't be worked out, which never fails a day. */
  static Set<Continent> continentsOf(RoundContinents continents, GameData data) {
    Set<Continent> result = EnumSet.noneOf(Continent.class);
    try {
      continents.of(data).forEach(label -> result.add(Continent.fromLabel(label)));
    } catch (RuntimeException e) {
      result.clear();
    }
    return result;
  }

  /**
   * How lopsided a day is, lower is better: 2 for each continent in four or more of today's
   * rounds, and 1 for each continent that is in three or more rounds today and was in three or
   * more on one of the last two days.
   */
  static int continentPenalty(List<Set<Continent>> today, DailyHistory history) {
    int penalty = 0;
    for (Continent continent : Continent.values()) {
      int count = countOf(today, continent);
      if (count >= 4) {
        penalty += 2;
      }
      if (count >= 3) {
        for (PastDay day : history.days()) {
          if (day.daysAgo() <= CONTINENT_DAYS && countOf(day.roundContinents(), continent) >= 3) {
            penalty++;
          }
        }
      }
    }
    return penalty;
  }

  private static int countOf(List<Set<Continent>> rounds, Continent continent) {
    return (int) rounds.stream().filter(set -> set.contains(continent)).count();
  }

  /**
   * The first 8 bytes of a SHA-256 of the date. Not {@code toEpochDay()}: {@code java.util.Random}
   * gives very similar first values for neighbouring seeds, so consecutive days would start alike.
   */
  static long seedFor(LocalDate date) {
    try {
      byte[] hash =
          MessageDigest.getInstance("SHA-256")
              .digest(("geogauntlet-daily:" + date).getBytes(StandardCharsets.UTF_8));
      return ByteBuffer.wrap(hash).getLong();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
