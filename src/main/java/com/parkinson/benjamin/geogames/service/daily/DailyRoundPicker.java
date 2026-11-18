package com.parkinson.benjamin.geogames.service.daily;

import com.parkinson.benjamin.geogames.model.GameData;
import com.parkinson.benjamin.geogames.model.GameType;
import com.parkinson.benjamin.geogames.service.RoundGenerators;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Chooses the five rounds of a day's challenge from a seed made of the date: five different game
 * types (never the extra difficult ones), each about a different subject. The same date gives the
 * same rounds for the same code and data; {@link DailyChallengeStore} pins them so a deploy can't
 * change a day that has started.
 */
@Service
public class DailyRoundPicker {

  public static final int DAILY_ROUNDS = 5;

  private final RoundGenerators generators;
  private final List<GameType> pool;

  public DailyRoundPicker(RoundGenerators generators) {
    this.generators = generators;
    this.pool = Arrays.stream(GameType.values()).filter(type -> !type.extraDifficult()).toList();
    if (pool.size() < DAILY_ROUNDS) {
      throw new IllegalStateException(
          "The daily challenge needs %d game types that aren't extra difficult, found %d"
              .formatted(DAILY_ROUNDS, pool.size()));
    }
  }

  public List<GameData> pick(LocalDate date) {
    Random random = new Random(seedFor(date));
    List<GameType> types = new ArrayList<>(pool);
    Collections.shuffle(types, random);

    List<GameData> rounds = new ArrayList<>(DAILY_ROUNDS);
    Set<String> used = new HashSet<>();
    for (GameType type : types.subList(0, DAILY_ROUNDS)) {
      GameData round = generators.generate(type, 1, Set.copyOf(used), random).get(0);
      used.add(round.subjectId());
      rounds.add(round);
    }
    return rounds;
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
