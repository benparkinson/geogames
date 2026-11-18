package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.helper.RandomPicker;
import com.parkinson.benjamin.geogames.model.GameData;
import com.parkinson.benjamin.geogames.model.GameType;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

/**
 * A generator whose rounds are each about one item, named in the round key by a prefix and an id.
 *
 * <p>{@code generate} and {@code rebuild} live here so they can't drift apart. The candidate order
 * and the single {@link RandomPicker#pickDistinct} call decide which rounds a seeded (daily)
 * challenge picks, so don't change them.
 */
abstract class SubjectRoundGenerator<S> implements RoundGenerator {

  private final GameType gameType;
  private final String prefix;
  private final String noun;

  SubjectRoundGenerator(GameType gameType, String prefix, String noun) {
    this.gameType = gameType;
    this.prefix = prefix;
    this.noun = noun;
  }

  /** Every eligible subject, in a stable order. */
  abstract List<S> candidates();

  abstract String id(S subject);

  /** The subject with this id, eligible or not: the round was eligible when the game was made. */
  abstract Optional<S> find(String id);

  abstract GameData toRound(S subject);

  @Override
  public final GameType gameType() {
    return gameType;
  }

  @Override
  public final List<GameData> generate(int count, Set<String> excludedSubjects, Random random) {
    List<S> available =
        candidates().stream()
            .filter(subject -> !excludedSubjects.contains(prefix + id(subject)))
            .toList();
    return RandomPicker.pickDistinct(available, count, random, noun).stream()
        .map(this::toRound)
        .toList();
  }

  @Override
  public final GameData rebuild(String roundKey) {
    if (!roundKey.startsWith(prefix)) {
      throw new RoundDataMissingException(gameType, roundKey);
    }
    return find(roundKey.substring(prefix.length()))
        .map(this::toRound)
        .orElseThrow(() -> new RoundDataMissingException(gameType, roundKey));
  }
}
