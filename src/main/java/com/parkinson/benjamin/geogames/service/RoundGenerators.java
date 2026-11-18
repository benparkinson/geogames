package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.model.GameData;
import com.parkinson.benjamin.geogames.model.GameType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Every game type's {@link RoundGenerator}, looked up by type. */
@Component
public class RoundGenerators {

  private final Map<GameType, RoundGenerator> generators = new EnumMap<>(GameType.class);

  /**
   * @throws IllegalStateException if a game type has no generator, or has two
   */
  public RoundGenerators(List<RoundGenerator> roundGenerators) {
    for (RoundGenerator generator : roundGenerators) {
      if (generators.put(generator.gameType(), generator) != null) {
        throw new IllegalStateException(
            "More than one round generator for game type " + generator.gameType());
      }
    }
    for (GameType type : GameType.values()) {
      if (!generators.containsKey(type)) {
        throw new IllegalStateException("No round generator for game type " + type);
      }
    }
  }

  public List<GameData> generate(
      GameType type, int count, Set<String> excludedSubjects, Random random) {
    return generators.get(type).generate(count, excludedSubjects, random);
  }

  /** Gives each round a random game type, then shuffles so the types come in a random order. */
  public List<GameData> generateMixed(int numberOfRounds, Random random) {
    GameType[] types = GameType.values();
    Map<GameType, Integer> roundsPerType = new EnumMap<>(GameType.class);
    for (int i = 0; i < numberOfRounds; i++) {
      roundsPerType.merge(types[random.nextInt(types.length)], 1, Integer::sum);
    }

    List<GameData> allGameData = new ArrayList<>(numberOfRounds);
    Set<String> used = new HashSet<>();
    roundsPerType.forEach(
        (type, count) -> {
          List<GameData> generated = generate(type, count, Set.copyOf(used), random);
          generated.forEach(data -> used.add(data.subjectId()));
          allGameData.addAll(generated);
        });
    Collections.shuffle(allGameData, random);
    return allGameData;
  }

  public GameData rebuild(GameType type, String roundKey) {
    return generators.get(type).rebuild(roundKey);
  }
}
