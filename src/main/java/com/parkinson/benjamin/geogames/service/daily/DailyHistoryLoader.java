package com.parkinson.benjamin.geogames.service.daily;

import com.parkinson.benjamin.geogames.model.Continent;
import com.parkinson.benjamin.geogames.model.GameData;
import com.parkinson.benjamin.geogames.model.GameType;
import com.parkinson.benjamin.geogames.service.RoundContinents;
import com.parkinson.benjamin.geogames.service.RoundDataMissingException;
import com.parkinson.benjamin.geogames.service.RoundGenerators;
import com.parkinson.benjamin.geogames.service.daily.DailyHistory.PastDay;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Builds the {@link DailyHistory} for a date from the pinned days before it. The subjects and
 * continents aren't stored: each round is rebuilt from its key (a few dozen rebuilds, once per
 * generated day) and nothing is cached.
 */
@Component
public class DailyHistoryLoader {

  private final DailyChallengeStore store;
  private final RoundGenerators generators;
  private final RoundContinents continents;

  public DailyHistoryLoader(
      DailyChallengeStore store, RoundGenerators generators, RoundContinents continents) {
    this.store = store;
    this.generators = generators;
    this.continents = continents;
  }

  /** The stored days before {@code date}, nearest first. Missing days are simply absent. */
  DailyHistory load(LocalDate date) {
    List<PastDay> days = new ArrayList<>();
    for (Map.Entry<LocalDate, List<PinnedRound>> entry :
        store.findBefore(date, DailyRoundPicker.SUBJECT_DAYS).entrySet()) {
      int daysAgo = (int) ChronoUnit.DAYS.between(entry.getKey(), date);
      Set<GameType> types = EnumSet.noneOf(GameType.class);
      Set<String> subjects = new HashSet<>();
      List<Set<Continent>> roundContinents = new ArrayList<>();
      for (PinnedRound pinned : entry.getValue()) {
        types.add(pinned.gameType());
        try {
          GameData data = generators.rebuild(pinned.gameType(), pinned.roundKey());
          subjects.add(data.subjectId());
          roundContinents.add(DailyRoundPicker.continentsOf(continents, data));
        } catch (RoundDataMissingException gone) {
          // The data behind an old day has gone: that must never stop a new day being made.
        }
      }
      days.add(new PastDay(daysAgo, types, subjects, roundContinents));
    }
    return new DailyHistory(days);
  }
}
