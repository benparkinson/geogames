package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.helper.RandomPicker;
import com.parkinson.benjamin.geogames.model.Country;
import com.parkinson.benjamin.geogames.model.GameData;
import com.parkinson.benjamin.geogames.model.GameType;
import com.parkinson.benjamin.geogames.model.River;
import com.parkinson.benjamin.geogames.model.RiverRound;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Makes the rounds for one of the river game types. They all draw on the same rivers and differ in
 * which rivers are eligible and whether countries are attached, so {@code RiverGamesConfig}
 * registers one of these per type.
 */
public class RiverRoundGenerator implements RoundGenerator {

  private final GameType gameType;
  private final RiverLoaderService riverLoaderService;
  private final Random random;
  private final Predicate<River> eligible;
  private final Function<River, List<Country>> countries;

  public RiverRoundGenerator(
      GameType gameType,
      RiverLoaderService riverLoaderService,
      Random random,
      Predicate<River> eligible,
      Function<River, List<Country>> countries) {
    this.gameType = gameType;
    this.riverLoaderService = riverLoaderService;
    this.random = random;
    this.eligible = eligible;
    this.countries = countries;
  }

  @Override
  public GameType gameType() {
    return gameType;
  }

  @Override
  public List<GameData> generate(int count, Set<String> excludedSubjects) {
    List<River> candidates =
        riverLoaderService.loadRivers().stream()
            .filter(eligible)
            .filter(river -> !excludedSubjects.contains("river:" + river.id()))
            .toList();
    return RandomPicker.pickDistinct(candidates, count, random, "rivers").stream()
        .<GameData>map(river -> new RiverRound(gameType, river, countries.apply(river)))
        .toList();
  }
}
