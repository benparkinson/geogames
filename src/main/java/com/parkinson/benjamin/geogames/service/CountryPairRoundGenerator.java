package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.helper.CountryPairEligibility;
import com.parkinson.benjamin.geogames.helper.RandomPicker;
import com.parkinson.benjamin.geogames.model.Bounds;
import com.parkinson.benjamin.geogames.model.CountryOutline;
import com.parkinson.benjamin.geogames.model.CountryPairRound;
import com.parkinson.benjamin.geogames.model.CountryShape;
import com.parkinson.benjamin.geogames.model.GameData;
import com.parkinson.benjamin.geogames.model.GameType;
import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import com.parkinson.benjamin.geogames.model.geojson.GeoGeometry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiPredicate;

/**
 * Makes the rounds for one of the pair game types. They draw on the same neighbouring countries and
 * differ in which pairs are eligible and whether the surrounding countries are attached, so {@code
 * CountryPairGamesConfig} registers one of these per type.
 *
 * <p>Nothing is built until a pair is picked: {@code generate} only lists pairs of the cached
 * shapes, and a round holds references to them.
 */
public class CountryPairRoundGenerator implements RoundGenerator {

  private static final String PREFIX = "pair:";

  /** Two countries, the one that comes first by name first. */
  private record Pair(CountryShape first, CountryShape second) {}

  private final GameType gameType;
  private final CountryShapeService shapes;
  private final BiPredicate<CountryShape, CountryShape> eligible;
  private final boolean withNeighbours;

  public CountryPairRoundGenerator(
      GameType gameType,
      CountryShapeService shapes,
      BiPredicate<CountryShape, CountryShape> eligible,
      boolean withNeighbours) {
    this.gameType = gameType;
    this.shapes = shapes;
    this.eligible = eligible;
    this.withNeighbours = withNeighbours;
  }

  @Override
  public GameType gameType() {
    return gameType;
  }

  @Override
  public List<GameData> generate(int count, Set<String> excludedSubjects, Random random) {
    List<Pair> candidates = new ArrayList<>();
    List<CountryShape> all = shapes.loadShapes();
    for (CountryShape first : all) {
      for (CountryShape second : all) {
        if (first.name().compareTo(second.name()) < 0
            && !excludedSubjects.contains(CountryPairRound.subjectId(first.name(), second.name()))
            && eligible.test(first, second)) {
          candidates.add(new Pair(first, second));
        }
      }
    }
    return RandomPicker.pickDistinct(candidates, count, random, "country pairs").stream()
        .<GameData>map(this::toRound)
        .toList();
  }

  /** Doesn't check {@code eligible}: the round was eligible when the game was created. */
  @Override
  public GameData rebuild(String roundKey) {
    if (!roundKey.startsWith(PREFIX)) {
      throw new RoundDataMissingException(gameType, roundKey);
    }
    String[] names = roundKey.substring(PREFIX.length()).split("\\|", -1);
    if (names.length != 2) {
      throw new RoundDataMissingException(gameType, roundKey);
    }
    CountryShape first = find(names[0], roundKey);
    CountryShape second = find(names[1], roundKey);
    if (first.name().compareTo(second.name()) >= 0
        || CountryPairEligibility.neighbour(first, second) == null) {
      throw new RoundDataMissingException(gameType, roundKey);
    }
    return toRound(new Pair(first, second));
  }

  private CountryShape find(String name, String roundKey) {
    return shapes
        .findShape(name)
        .orElseThrow(() -> new RoundDataMissingException(gameType, roundKey));
  }

  private CountryPairRound toRound(Pair pair) {
    CountryShape first = pair.first();
    CountryShape second = pair.second();
    CountryShape.Neighbour neighbour = CountryPairEligibility.neighbour(first, second);
    GeoData sharedBorder = sharedBorder(first, neighbour, second.name());
    return new CountryPairRound(
        gameType,
        List.of(first, second),
        sharedBorder,
        Math.round(neighbour.km()),
        withNeighbours ? surroundings(first, second) : List.of(),
        first.focus().union(second.focus()),
        Bounds.of(sharedBorder));
  }

  /** The first country's lines for the second, copied out of its land borders. */
  private static GeoData sharedBorder(
      CountryShape first, CountryShape.Neighbour neighbour, String secondName) {
    List<?> lines = first.landBorders().geometry().coordinates();
    List<Object> picked = new ArrayList<>();
    for (int index : neighbour.lines()) {
      picked.add(lines.get(index));
    }
    return GeoData.feature(
        first.name() + "|" + secondName, new GeoGeometry("MultiLineString", picked));
  }

  /** The mainland neighbours of either country, except the pair, each once, in name order. */
  private List<CountryOutline> surroundings(CountryShape first, CountryShape second) {
    Map<String, CountryOutline> byName = new TreeMap<>();
    for (CountryShape country : List.of(first, second)) {
      for (CountryShape.Neighbour n : country.neighbours()) {
        if (n.mainland()
            && !n.name().equals(first.name())
            && !n.name().equals(second.name())
            && !byName.containsKey(n.name())) {
          byName.put(n.name(), new CountryOutline(n.name(), shapes.loadShape(n.name()).outline()));
        }
      }
    }
    return List.copyOf(byName.values());
  }
}
