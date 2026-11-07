package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.model.GameData;
import com.parkinson.benjamin.geogames.model.GameType;
import java.util.List;
import java.util.Set;

/**
 * Makes the rounds for one {@link GameType}. {@link GameService} finds every generator bean by
 * type, so a new game type needs one of these and nothing else in the service.
 */
public interface RoundGenerator {

  GameType gameType();

  /**
   * Returns {@code count} distinct rounds, none of whose {@link GameData#subjectId()} is in {@code
   * excludedSubjects}.
   *
   * @throws IllegalArgumentException if there isn't enough data for that many
   */
  List<GameData> generate(int count, Set<String> excludedSubjects);
}
