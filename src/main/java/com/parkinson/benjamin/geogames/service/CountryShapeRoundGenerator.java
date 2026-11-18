package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.model.CountryOutline;
import com.parkinson.benjamin.geogames.model.CountryShape;
import com.parkinson.benjamin.geogames.model.CountryShapeRound;
import com.parkinson.benjamin.geogames.model.GameData;
import com.parkinson.benjamin.geogames.model.GameType;
import com.parkinson.benjamin.geogames.model.SubjectIds;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Makes the rounds for one of the country-shape game types. They all draw on the same countries
 * and differ in which are eligible and whether the neighbours' outlines are attached, so {@code
 * CountryGamesConfig} registers one of these per type.
 */
public class CountryShapeRoundGenerator extends SubjectRoundGenerator<CountryShape> {

  private final CountryShapeService shapes;
  private final Predicate<CountryShape> eligible;
  private final boolean withNeighbours;

  public CountryShapeRoundGenerator(
      GameType gameType,
      CountryShapeService shapes,
      Predicate<CountryShape> eligible,
      boolean withNeighbours) {
    super(gameType, SubjectIds.COUNTRY, "countries");
    this.shapes = shapes;
    this.eligible = eligible;
    this.withNeighbours = withNeighbours;
  }

  @Override
  List<CountryShape> candidates() {
    return shapes.loadShapes().stream().filter(eligible).toList();
  }

  @Override
  String id(CountryShape shape) {
    return shape.name();
  }

  /** Doesn't check {@code eligible}: the round was eligible when the game was created. */
  @Override
  Optional<CountryShape> find(String name) {
    return shapes.findShape(name);
  }

  @Override
  GameData toRound(CountryShape shape) {
    return new CountryShapeRound(
        gameType(), shape, withNeighbours ? neighboursOf(shape) : List.of());
  }

  /** The mainland neighbours' outlines, longest border first. */
  private List<CountryOutline> neighboursOf(CountryShape shape) {
    return shape.neighbours().stream()
        .filter(CountryShape.Neighbour::mainland)
        .map(n -> new CountryOutline(n.name(), shapes.loadShape(n.name()).outline()))
        .toList();
  }
}
