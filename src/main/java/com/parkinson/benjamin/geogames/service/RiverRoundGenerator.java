package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.model.Bounds;
import com.parkinson.benjamin.geogames.model.Country;
import com.parkinson.benjamin.geogames.model.GameData;
import com.parkinson.benjamin.geogames.model.GameType;
import com.parkinson.benjamin.geogames.model.River;
import com.parkinson.benjamin.geogames.model.RiverRound;
import com.parkinson.benjamin.geogames.model.SubjectIds;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Makes the rounds for one of the river game types. They all draw on the same rivers and differ in
 * which rivers are eligible and whether countries are attached, so {@code RiverGamesConfig}
 * registers one of these per type.
 */
public class RiverRoundGenerator extends SubjectRoundGenerator<River> {

  private final RiverLoaderService riverLoaderService;
  private final Predicate<River> eligible;
  private final Function<River, List<Country>> countries;
  private final Function<River, Bounds> focus;

  public RiverRoundGenerator(
      GameType gameType,
      RiverLoaderService riverLoaderService,
      Predicate<River> eligible,
      Function<River, List<Country>> countries,
      Function<River, Bounds> focus) {
    super(gameType, SubjectIds.RIVER, "rivers");
    this.riverLoaderService = riverLoaderService;
    this.eligible = eligible;
    this.countries = countries;
    this.focus = focus;
  }

  @Override
  List<River> candidates() {
    return riverLoaderService.loadRivers().stream().filter(eligible).toList();
  }

  @Override
  String id(River river) {
    return river.id();
  }

  /** Doesn't check {@code eligible}: the round was eligible when the game was created. */
  @Override
  Optional<River> find(String id) {
    return riverLoaderService.loadRivers().stream()
        .filter(river -> river.id().equals(id))
        .findFirst();
  }

  @Override
  GameData toRound(River river) {
    return new RiverRound(gameType(), river, countries.apply(river), focus.apply(river));
  }
}
