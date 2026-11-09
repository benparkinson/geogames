package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.helper.RandomPicker;
import com.parkinson.benjamin.geogames.model.CountryOutline;
import com.parkinson.benjamin.geogames.model.CountryShape;
import com.parkinson.benjamin.geogames.model.CountryShapeRound;
import com.parkinson.benjamin.geogames.model.GameData;
import com.parkinson.benjamin.geogames.model.GameType;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Makes the rounds for one of the country-shape game types. They all draw on the same countries
 * and differ in which are eligible and whether the neighbours' outlines are attached, so {@code
 * CountryGamesConfig} registers one of these per type.
 */
public class CountryShapeRoundGenerator implements RoundGenerator {

  private static final String PREFIX = "country:";

  private final GameType gameType;
  private final CountryShapeService shapes;
  private final Random random;
  private final Predicate<CountryShape> eligible;
  private final boolean withNeighbours;

  public CountryShapeRoundGenerator(
      GameType gameType,
      CountryShapeService shapes,
      Random random,
      Predicate<CountryShape> eligible,
      boolean withNeighbours) {
    this.gameType = gameType;
    this.shapes = shapes;
    this.random = random;
    this.eligible = eligible;
    this.withNeighbours = withNeighbours;
  }

  @Override
  public GameType gameType() {
    return gameType;
  }

  @Override
  public List<GameData> generate(int count, Set<String> excludedSubjects) {
    List<CountryShape> candidates =
        shapes.loadShapes().stream()
            .filter(eligible)
            .filter(shape -> !excludedSubjects.contains("country:" + shape.name()))
            .toList();
    return RandomPicker.pickDistinct(candidates, count, random, "countries").stream()
        .<GameData>map(this::toRound)
        .toList();
  }

  /** Doesn't check {@code eligible}: the round was eligible when the game was created. */
  @Override
  public GameData rebuild(String roundKey) {
    if (!roundKey.startsWith(PREFIX)) {
      throw new RoundDataMissingException(gameType, roundKey);
    }
    String name = roundKey.substring(PREFIX.length());
    return shapes.loadShapes().stream()
        .filter(shape -> shape.name().equals(name))
        .findFirst()
        .map(this::toRound)
        .orElseThrow(() -> new RoundDataMissingException(gameType, roundKey));
  }

  private CountryShapeRound toRound(CountryShape shape) {
    return new CountryShapeRound(gameType, shape, withNeighbours ? neighboursOf(shape) : List.of());
  }

  /** The mainland neighbours' outlines, longest border first. */
  private List<CountryOutline> neighboursOf(CountryShape shape) {
    return shape.neighbours().stream()
        .filter(CountryShape.Neighbour::mainland)
        .map(n -> new CountryOutline(n.name(), shapes.loadShape(n.name()).outline()))
        .toList();
  }
}
