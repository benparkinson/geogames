package com.parkinson.benjamin.geogames.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.parkinson.benjamin.geogames.helper.CountryCitiesEligibility;
import com.parkinson.benjamin.geogames.helper.CountryPairEligibility;
import com.parkinson.benjamin.geogames.helper.CountryRiversEligibility;
import com.parkinson.benjamin.geogames.helper.CountryShapeEligibility;
import com.parkinson.benjamin.geogames.helper.PolygonCentroid;
import com.parkinson.benjamin.geogames.helper.RiverCountriesEligibility;
import com.parkinson.benjamin.geogames.helper.RiverPosition;
import com.parkinson.benjamin.geogames.loaders.features.AliasBuilder;
import com.parkinson.benjamin.geogames.loaders.features.Geo;
import com.parkinson.benjamin.geogames.model.Bounds;
import com.parkinson.benjamin.geogames.model.City;
import com.parkinson.benjamin.geogames.model.Country;
import com.parkinson.benjamin.geogames.model.CountryCities;
import com.parkinson.benjamin.geogames.model.CountryPairRound;
import com.parkinson.benjamin.geogames.model.CountryRivers;
import com.parkinson.benjamin.geogames.model.CountryRiversIndexEntry;
import com.parkinson.benjamin.geogames.model.CountryShape;
import com.parkinson.benjamin.geogames.model.Lake;
import com.parkinson.benjamin.geogames.model.LocatedPlaceName;
import com.parkinson.benjamin.geogames.model.PlaceName;
import com.parkinson.benjamin.geogames.model.River;
import com.parkinson.benjamin.geogames.model.Tripoint;
import com.parkinson.benjamin.geogames.model.TripointDefinition;
import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import com.parkinson.benjamin.geogames.model.geojson.Position;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

public class ProcessedDataIT {

  // Tripoints with no border junction nearby, removed during the country file migration.
  private static final List<Set<String>> DROPPED =
      List.of(
          Set.of("Somalia", "Djibouti", "Ethiopia"),
          Set.of("Ethiopia", "South Sudan", "Kenya"),
          Set.of("Azerbaijan", "Iran", "Turkmenistan"),
          Set.of("Russia", "Kazakhstan", "Azerbaijan"),
          Set.of("Kazakhstan", "Azerbaijan", "Turkmenistan"),
          Set.of("Pakistan", "India", "China"));

  private final JsonResourceReader reader =
      new JsonResourceReader(new PathMatchingResourcePatternResolver());
  private final TripointLoaderService tripointLoaderService = new TripointLoaderService(reader);
  private final CountryShapeService shapeService = new CountryShapeService(reader);

  @Test
  public void tripointsAreWellFormed() throws IOException {
    List<TripointDefinition> definitions = tripointLoaderService.loadTripointDefinitions();

    assertEquals(164, definitions.size());
    for (TripointDefinition definition : definitions) {
      Set<String> names = new HashSet<>(definition.countryNames());
      assertEquals(3, names.size(), "expected 3 distinct countries in " + definition);
      assertFalse(DROPPED.contains(names), "dropped tripoint is back: " + names);
    }
  }

  @Test
  public void roundKeysAreUniqueWithinEachGameTypeAndFitTheColumn() {
    List<String> tripointKeys =
        tripointLoaderService.loadTripointDefinitions().stream().map(Tripoint::roundKey).toList();
    assertUniqueAndShort("tripoint", tripointKeys);
    assertUniqueAndShort(
        "river",
        new RiverLoaderService(reader).loadRivers().stream().map(r -> "river:" + r.id()).toList());
    assertUniqueAndShort("lake", loadLakes().stream().map(l -> "lake:" + l.id()).toList());
    assertUniqueAndShort(
        "country", shapeService.loadShapes().stream().map(s -> "country:" + s.name()).toList());
    assertUniqueAndShort(
        "country rivers",
        new CountryRiversService(reader).index().stream().map(e -> "country:" + e.name()).toList());
    assertUniqueAndShort(
        "cities",
        new CityService(reader).loadAll().stream().map(c -> "country:" + c.country()).toList());
    assertUniqueAndShort(
        "pair",
        pairs(CountryPairEligibility.borders()).stream()
            .map(p -> CountryPairRound.subjectId(p[0].name(), p[1].name()))
            .toList());
  }

  /** Every pair, the country that comes first by name first, for which the test passes. */
  private List<CountryShape[]> pairs(BiPredicate<CountryShape, CountryShape> test) {
    List<CountryShape> all = shapeService.loadShapes();
    List<CountryShape[]> pairs = new java.util.ArrayList<>();
    for (CountryShape first : all) {
      for (CountryShape second : all) {
        if (first.name().compareTo(second.name()) < 0 && test.test(first, second)) {
          pairs.add(new CountryShape[] {first, second});
        }
      }
    }
    return pairs;
  }

  @Test
  public void eachPairGameHasEnoughPairs() {
    int borders = pairs(CountryPairEligibility.borderShapes()).size();
    int merged = pairs(CountryPairEligibility.mergedCountries()).size();

    assertTrue(borders >= 150, "only " + borders + " pairs for Border Shapes");
    assertTrue(merged >= 150, "only " + merged + " pairs for Merged Countries");
  }

  private static void assertUniqueAndShort(String type, List<String> keys) {
    assertEquals(keys.size(), new HashSet<>(keys).size(), "duplicate " + type + " round key");
    for (String key : keys) {
      assertTrue(key.length() <= 255, type + " key too long for the column: " + key);
    }
  }

  @Test
  public void everyTripointCountryLoads() throws IOException {
    for (TripointDefinition definition : tripointLoaderService.loadTripointDefinitions()) {
      for (String name : definition.countryNames()) {
        assertDisplayCountryLoads(name);
      }
    }
  }

  private void assertDisplayCountryLoads(String name) {
    Country display = shapeService.loadDisplayCountry(name);
    assertEquals(name, display.name());
    assertNotNull(display.geoData().geometry(), "no outline for " + name);
  }

  @Test
  public void everyTripointIsAVertexOfEachOfItsCountriesOutlines() {
    StringBuilder failures = new StringBuilder();
    for (TripointDefinition definition : tripointLoaderService.loadTripointDefinitions()) {
      double[] point = {definition.coordinate().longitude(), definition.coordinate().latitude()};
      for (String name : definition.countryNames()) {
        double km = nearestVertexKm(shapeService.loadDisplayCountry(name), point);
        if (km > 1.0) {
          failures.append(
              String.format("%s: %.1f km from %s%n", definition.countryNames(), km, name));
        }
      }
    }
    assertEquals("", failures.toString(), "tripoints too far from a simplified outline vertex");
  }

  private static double nearestVertexKm(Country country, double[] point) {
    double best = Double.MAX_VALUE;
    for (double[] vertex : vertices(country.geoData().geometry().coordinates())) {
      best = Math.min(best, Geo.haversineKm(point, vertex));
    }
    return best;
  }

  /** Every [lon, lat] position in nested GeoJSON coordinates. */
  private static List<double[]> vertices(Object node) {
    List<double[]> out = new java.util.ArrayList<>();
    List<?> list = (List<?>) node;
    if (!list.isEmpty() && list.get(0) instanceof Number) {
      out.add(
          new double[] {
            ((Number) list.get(0)).doubleValue(), ((Number) list.get(1)).doubleValue()
          });
    } else {
      for (Object child : list) {
        out.addAll(vertices(child));
      }
    }
    return out;
  }

  @Test
  public void riversHaveEndsAndCountriesThatLoad() {
    List<River> rivers = new RiverLoaderService(reader).loadRivers();

    assertEquals(50, rivers.size());
    for (River river : rivers) {
      assertNotNull(river.source(), "no source for " + river.name());
      assertNotNull(river.mouth(), "no mouth for " + river.name());
      assertNotEquals(river.source(), river.mouth(), "source is mouth for " + river.name());
      assertNotNull(river.countries(), "no countries for " + river.name());
      for (String name : river.countries()) {
        assertDisplayCountryLoads(name);
      }
    }
  }

  @Test
  public void enoughRiversForTheCountriesGame() {
    long eligible =
        new RiverLoaderService(reader)
            .loadRivers().stream().filter(RiverCountriesEligibility.riverCountries()).count();

    assertTrue(eligible >= 10, "only " + eligible + " rivers are eligible for Follow the River");
  }

  @Test
  public void everyExcludedRiverCountriesIdIsARealRiver() {
    Set<String> ids = new HashSet<>();
    new RiverLoaderService(reader).loadRivers().forEach(river -> ids.add(river.id()));

    assertTrue(ids.containsAll(RiverCountriesEligibility.EXCLUDED));
  }

  @Test
  public void everyRiverPositionIsOneOfItsVertices() {
    RiverLoaderService service = new RiverLoaderService(reader);
    for (River river : service.loadRivers()) {
      double[] position = RiverPosition.of(river.geoData());
      boolean[] found = {false};
      river
          .geoData()
          .geometry()
          .forEachPosition(
              (lon, lat) -> {
                if (Math.abs(lon - position[1]) < 0.006 && Math.abs(lat - position[0]) < 0.006) {
                  found[0] = true;
                }
              });
      assertTrue(found[0], river.name() + " position is not one of its vertices");
    }
    assertEquals(service.loadRivers().size(), service.loadRiverNames().size());
  }

  @Test
  public void everyLakePositionIsInsideItsBoundingBox() {
    for (Lake lake : loadLakes()) {
      double[] position = PolygonCentroid.of(lake.geoData());
      double[] box = {180, 90, -180, -90};
      lake.geoData()
          .geometry()
          .forEachPosition(
              (lon, lat) -> {
                box[0] = Math.min(box[0], lon);
                box[1] = Math.min(box[1], lat);
                box[2] = Math.max(box[2], lon);
                box[3] = Math.max(box[3], lat);
              });
      assertTrue(
          position[1] >= box[0] - 0.01
              && position[1] <= box[2] + 0.01
              && position[0] >= box[1] - 0.01
              && position[0] <= box[3] + 0.01,
          lake.name() + " position is outside its bounding box");
    }
  }

  @Test
  public void noTwoRiversShareANormalisedNameOrAlias() {
    Set<String> seen = new HashSet<>();
    for (River river : new RiverLoaderService(reader).loadRivers()) {
      Set<String> mine = new HashSet<>();
      mine.add(AliasBuilder.normalise(river.name()));
      river.additionalNames().forEach(alias -> mine.add(AliasBuilder.normalise(alias)));
      for (String normalised : mine) {
        assertTrue(seen.add(normalised), "'" + normalised + "' is shared, one is " + river.name());
      }
    }
  }

  @Test
  public void noTwoLakesShareANormalisedNameOrAlias() {
    Set<String> seen = new HashSet<>();
    for (Lake lake : loadLakes()) {
      Set<String> mine = new HashSet<>();
      mine.add(AliasBuilder.normalise(lake.name()));
      lake.additionalNames().forEach(alias -> mine.add(AliasBuilder.normalise(alias)));
      for (String normalised : mine) {
        assertTrue(seen.add(normalised), "'" + normalised + "' is shared, one is " + lake.name());
      }
    }
  }

  @Test
  public void everyCoordinateReadFromTheProcessedDataIsAPosition() {
    int points = 0;
    for (CountryShape shape : shapeService.loadShapes()) {
      points += assertPositions(shape.outline(), shape.name());
      points += assertPositions(shape.coast(), shape.name());
      points += assertPositions(shape.landBorders(), shape.name());
    }
    for (River river : new RiverLoaderService(reader).loadRivers()) {
      points += assertPositions(river.geoData(), river.name());
    }
    for (Lake lake : loadLakes()) {
      points += assertPositions(lake.geoData(), lake.name());
    }
    assertTrue(points > 100_000, "only " + points + " points");
  }

  /** Checks every innermost list under the geometry is a Position, and returns how many there are. */
  private static int assertPositions(GeoData data, String name) {
    return data == null ? 0 : assertPositions(data.geometry().coordinates(), name);
  }

  private static int assertPositions(List<?> list, String name) {
    if (list.isEmpty()) {
      return 0;
    }
    if (list.get(0) instanceof Number) {
      assertTrue(list instanceof Position, "not a Position in " + name + ": " + list);
      return 1;
    }
    int count = 0;
    for (Object child : list) {
      count += assertPositions((List<?>) child, name);
    }
    return count;
  }

  private List<Lake> loadLakes() {
    return reader.readAll("classpath*:/processed/lakes/*.json", Lake.class);
  }

  /** The names in the raw {@code processed/countries} files, which the app itself never reads. */
  private List<PlaceName> rawCountryNames() {
    return reader.readAll("classpath*:/processed/countries/*.json", PlaceName.class).stream()
        .map(
            c ->
                new PlaceName(
                    c.name(), c.additionalNames() == null ? List.of() : c.additionalNames()))
        .sorted(Comparator.comparing(PlaceName::name))
        .toList();
  }

  @Test
  public void lakesHaveContinentsCountriesAndClosedRings() {
    List<Lake> lakes = loadLakes();
    Set<String> countryNames =
        rawCountryNames().stream()
            .map(PlaceName::name)
            .collect(java.util.stream.Collectors.toSet());

    assertTrue(lakes.size() >= 20, "only " + lakes.size() + " lakes");
    assertEquals(lakes.size(), lakes.stream().map(Lake::id).distinct().count());
    for (Lake lake : lakes) {
      assertNotNull(lake.continent(), "no continent for " + lake.name());
      assertFalse(lake.countries().isEmpty(), "no countries for " + lake.name());
      for (String name : lake.countries()) {
        assertTrue(countryNames.contains(name), "unknown country " + name + " for " + lake.name());
      }

      String type = lake.geoData().geometry().type();
      List<?> coordinates = lake.geoData().geometry().coordinates();
      List<List<?>> polygons = new java.util.ArrayList<>();
      if (type.equals("Polygon")) {
        polygons.add(coordinates);
      } else {
        assertEquals("MultiPolygon", type, lake.name());
        coordinates.forEach(polygon -> polygons.add((List<?>) polygon));
      }
      int points = 0;
      for (List<?> polygon : polygons) {
        for (Object ring : polygon) {
          List<?> ringPoints = (List<?>) ring;
          assertTrue(ringPoints.size() >= 4, "short ring in " + lake.name());
          assertEquals(
              ringPoints.get(0),
              ringPoints.get(ringPoints.size() - 1),
              "open ring in " + lake.name());
          points += ringPoints.size();
        }
      }
      assertTrue(points <= 800, lake.name() + " has " + points + " points");
    }
  }

  // Canada is the largest, at about 32,000 points across its outline and coast.
  private static final int MAX_SHAPE_POINTS = 35_000;

  @Test
  public void countryShapesMatchTheProcessedCountries() {
    CountryShapeService shapes = new CountryShapeService(reader);
    List<PlaceName> countries =
        rawCountryNames().stream().filter(c -> !"Antarctica".equals(c.name())).toList();

    List<PlaceName> shapeNames =
        shapes.loadShapes().stream()
            .map(s -> new PlaceName(s.name(), s.additionalNames()))
            .sorted(Comparator.comparing(PlaceName::name))
            .toList();

    // Fails if CountryLoaderMain was re-run without CountryShapeMain.
    assertEquals(countries, shapeNames);
  }

  @Test
  public void everyCountryHasAPositionInsideItsFocus() {
    CountryNameService service = new CountryNameService(new CountryShapeService(reader));
    CountryShapeService shapes = new CountryShapeService(reader);
    for (LocatedPlaceName country : service.loadCountryNames()) {
      Bounds focus = shapes.loadShape(country.name()).focus();
      assertTrue(Double.isFinite(country.lat()) && Double.isFinite(country.lon()), country.name());
      assertTrue(
          country.lat() >= focus.south() - 1
              && country.lat() <= focus.north() + 1
              && country.lon() >= focus.west() - 1
              && country.lon() <= focus.east() + 1,
          country.name() + " is outside its focus");
    }
  }

  @Test
  public void countryShapeNeighboursAreSymmetric() {
    CountryShapeService shapes = new CountryShapeService(reader);
    for (CountryShape shape : shapes.loadShapes()) {
      for (CountryShape.Neighbour neighbour : shape.neighbours()) {
        CountryShape other = shapes.loadShape(neighbour.name());
        CountryShape.Neighbour back =
            other.neighbours().stream()
                .filter(n -> n.name().equals(shape.name()))
                .findFirst()
                .orElse(null);
        assertNotNull(back, neighbour.name() + " does not list " + shape.name());
        assertEquals(neighbour.km(), back.km(), 1.0, shape.name() + " and " + neighbour.name());
      }
    }
  }

  @Test
  public void everyCountryWithCitiesHasAShapeAndWellFormedCities() {
    CityService cityService = new CityService(reader);
    Set<String> shapeNames =
        shapeService.loadShapes().stream().map(CountryShape::name).collect(Collectors.toSet());
    List<CountryCities> all = cityService.loadAll();

    assertEquals(
        all.stream().map(CountryCities::country).sorted().toList(),
        all.stream().map(CountryCities::country).toList(),
        "countries not sorted by name");
    for (CountryCities country : all) {
      assertTrue(shapeNames.contains(country.country()), "no shape for " + country.country());
      assertFalse(country.cities().isEmpty(), "no cities for " + country.country());
      // The 40 largest, and every capital, which is at most a handful.
      assertTrue(country.cities().size() <= 46, country.country() + " has too many cities");
      int previous = Integer.MAX_VALUE;
      for (City city : country.cities()) {
        assertFalse(city.name().isBlank(), "blank city in " + country.country());
        assertTrue(city.lon() >= -180 && city.lon() <= 180, city.name() + " longitude");
        assertTrue(city.lat() >= -90 && city.lat() <= 90, city.name() + " latitude");
        assertTrue(city.population() >= 0, city.name() + " population");
        assertTrue(city.population() <= previous, country.country() + " not sorted by population");
        previous = city.population();
      }
    }
  }

  // Natural Earth marks no capital for these, so Hint 1 of City Map would have nothing to
  // highlight. None of them is eligible for the game today; the test says so if one becomes so.
  private static final Set<String> NO_CAPITAL_EXCEPTIONS = Set.of();

  @Test
  public void everyEligibleCountryHasACapitalOrIsAKnownException() {
    for (CountryCities country : new CityService(reader).loadAll()) {
      if (CountryCitiesEligibility.countryCities().test(country)) {
        assertTrue(
            country.cities().stream().anyMatch(City::capital)
                || NO_CAPITAL_EXCEPTIONS.contains(country.country()),
            country.country() + " has no capital flagged");
      }
    }
  }

  @Test
  public void enoughCountriesHaveEnoughCitiesForTheCitiesGame() {
    Set<String> shapeNames =
        shapeService.loadShapes().stream().map(CountryShape::name).collect(Collectors.toSet());
    long eligible =
        new CityService(reader)
            .loadAll().stream()
                .filter(CountryCitiesEligibility.countryCities())
                .filter(c -> shapeNames.contains(c.country()))
                .count();

    assertTrue(eligible >= 80, "only " + eligible + " countries have enough cities");
  }

  @Test
  public void everyCountryRiversEntryHasAFileAndAShapeAndFitsTheLimits() {
    CountryRiversService service = new CountryRiversService(reader);
    Set<String> shapeNames =
        shapeService.loadShapes().stream().map(CountryShape::name).collect(Collectors.toSet());
    List<CountryRiversIndexEntry> index = service.index();

    assertEquals(
        index.stream().map(CountryRiversIndexEntry::name).sorted().toList(),
        index.stream().map(CountryRiversIndexEntry::name).toList(),
        "index not sorted by name");
    for (CountryRiversIndexEntry entry : index) {
      assertTrue(shapeNames.contains(entry.name()), "no shape for " + entry.name());
      CountryRivers rivers = service.load(entry.name());
      assertEquals(entry.name(), rivers.name());
      assertEquals("MultiLineString", rivers.major().geometry().type());
      assertEquals("MultiLineString", rivers.minor().geometry().type());
      assertEquals("MultiPolygon", rivers.lakes().geometry().type());

      int riverPoints =
          rivers.major().geometry().positionCount() + rivers.minor().geometry().positionCount();
      assertTrue(riverPoints > 0, entry.name() + " has no river points");
      assertTrue(riverPoints <= 4_000, entry.name() + " has " + riverPoints + " river points");
      int lakePoints = rivers.lakes().geometry().positionCount();
      assertTrue(lakePoints <= 2_000, entry.name() + " has " + lakePoints + " lake points");
      assertEquals(
          entry.lakeCount() == 0, rivers.lakes().geometry().coordinates().isEmpty(), entry.name());
      assertTrue(entry.riverCount() > 0 && entry.riverKm() > 0, entry.name());
    }
  }

  @Test
  public void everyLakePolygonOfCountryRiversIsClosed() {
    CountryRiversService service = new CountryRiversService(reader);
    for (CountryRiversIndexEntry entry : service.index()) {
      for (Object polygon : service.load(entry.name()).lakes().geometry().coordinates()) {
        for (Object ring : (List<?>) polygon) {
          List<?> points = (List<?>) ring;
          assertTrue(points.size() >= 4, "short lake ring in " + entry.name());
          assertEquals(
              points.get(0), points.get(points.size() - 1), "open lake ring in " + entry.name());
        }
      }
    }
  }

  @Test
  public void enoughCountriesHaveEnoughRiversForTheRiversGame() {
    CountryRiversService service = new CountryRiversService(reader);
    Set<String> shapeNames =
        shapeService.loadShapes().stream().map(CountryShape::name).collect(Collectors.toSet());
    long eligible =
        service.index().stream()
            .filter(CountryRiversEligibility.countryRivers())
            .filter(e -> shapeNames.contains(e.name()))
            .count();

    assertTrue(eligible >= 60, "only " + eligible + " countries have enough rivers");
    assertTrue(eligible <= 110, eligible + " countries is too many for the rivers game");
  }

  @Test
  public void mostCountriesOfTheRiversGameNameTheirLongestRiverWithoutGivingTheCountryAway() {
    CountryRiversService service = new CountryRiversService(reader);
    List<CountryRiversIndexEntry> eligible =
        service.index().stream().filter(CountryRiversEligibility.countryRivers()).toList();
    long named = 0;
    for (CountryRiversIndexEntry entry : eligible) {
      String river = service.load(entry.name()).longestRiver();
      if (river == null) {
        continue;
      }
      named++;
      assertFalse(
          river.toLowerCase().contains(entry.name().toLowerCase()),
          river + " gives away " + entry.name());
    }

    assertTrue(named >= 60, "only " + named + " of " + eligible.size() + " have a longest river");
  }

  @Test
  public void countryAreasAreRoughlyRight() {
    CountryShapeService shapes = new CountryShapeService(reader);

    // France's mainland is about 548,000 km2.
    assertEquals(548_000, shapes.loadShape("France").areaKm2(), 548_000 * 0.02);
    for (CountryShape shape : shapes.loadShapes()) {
      // Vatican City is under a square kilometre, so it rounds to 0.
      assertTrue(shape.areaKm2() >= 0, shape.name() + " has a negative area");
      if (shape.landBorderKm() >= CountryPairEligibility.MIN_BORDER_KM) {
        assertTrue(shape.areaKm2() > 100, shape.name() + " has area " + shape.areaKm2());
      }
    }
  }

  @Test
  public void neighbourLinesPointAtTheirOwnBorderLinesAndNoOther() {
    CountryShapeService shapes = new CountryShapeService(reader);
    for (CountryShape shape : shapes.loadShapes()) {
      int lineCount = shape.landBorders().geometry().coordinates().size();
      Set<Integer> seen = new HashSet<>();
      for (CountryShape.Neighbour neighbour : shape.neighbours()) {
        String who = shape.name() + " and " + neighbour.name();
        assertFalse(neighbour.lines().isEmpty(), "no lines for " + who);
        assertEquals(
            neighbour.lines().stream().sorted().toList(), neighbour.lines(), "unsorted " + who);
        for (int index : neighbour.lines()) {
          assertTrue(index >= 0 && index < lineCount, "line " + index + " out of range: " + who);
          assertTrue(seen.add(index), "line " + index + " belongs to two neighbours: " + who);
        }
      }
    }
  }

  @Test
  public void bothCountriesOfABorderDrawTheSamePointsForIt() {
    CountryShapeService shapes = new CountryShapeService(reader);
    for (CountryShape shape : shapes.loadShapes()) {
      for (CountryShape.Neighbour neighbour : shape.neighbours()) {
        CountryShape other = shapes.loadShape(neighbour.name());
        CountryShape.Neighbour back =
            other.neighbours().stream()
                .filter(n -> n.name().equals(shape.name()))
                .findFirst()
                .orElseThrow();
        String who = shape.name() + " and " + neighbour.name();
        if (UNMATCHED_BORDERS.contains(who)) {
          continue;
        }
        List<double[]> mine = borderPoints(shape, neighbour);
        List<double[]> theirs = borderPoints(other, back);
        // The two countries' vertices on a border differ by up to a metre in the source, so the
        // ends of a run, which are not simplified away, may round to a different fourth decimal.
        assertEveryPointHasAMatch(mine, theirs, who);
        assertEveryPointHasAMatch(theirs, mine, who);
      }
    }
  }

  private static void assertEveryPointHasAMatch(
      List<double[]> points, List<double[]> others, String who) {
    for (double[] point : points) {
      assertTrue(
          others.stream()
              .anyMatch(
                  p ->
                      Math.abs(p[0] - point[0]) <= SAME_POINT_DEGREES
                          && Math.abs(p[1] - point[1]) <= SAME_POINT_DEGREES),
          "no matching border point for " + point[0] + "," + point[1] + " in " + who);
    }
  }

  /**
   * Borders whose two sides don't match, found when this test was written: of the 324 bordering
   * pairs, only Canada and the United States cut their border into different runs (five lines on
   * Canada's side, four on the United States'), so the simplified points differ by up to 0.2
   * degrees. The pair games only draw the first country's lines, so this only means the two copies
   * of this one border are not identical. Don't add to this list without finding out why.
   */
  private static final Set<String> UNMATCHED_BORDERS =
      Set.of("Canada and United States of America", "United States of America and Canada");

  /** One unit in the fourth decimal place, about 10 m. */
  private static final double SAME_POINT_DEGREES = 0.00011;

  private static List<double[]> borderPoints(CountryShape shape, CountryShape.Neighbour neighbour) {
    List<?> lines = shape.landBorders().geometry().coordinates();
    List<double[]> points = new java.util.ArrayList<>();
    for (int index : neighbour.lines()) {
      points.addAll(vertices(lines.get(index)));
    }
    return points;
  }

  @Test
  public void countryShapesAreWellFormed() {
    for (CountryShape shape : new CountryShapeService(reader).loadShapes()) {
      List<?> coordinates = shape.outline().geometry().coordinates();
      List<?> polygons =
          shape.outline().geometry().type().equals("Polygon") ? List.of(coordinates) : coordinates;
      for (Object polygon : polygons) {
        for (Object ring : (List<?>) polygon) {
          List<?> points = (List<?>) ring;
          assertTrue(points.size() >= 4, shape.name() + " has a ring with too few points");
          assertEquals(
              points.get(0), points.get(points.size() - 1), shape.name() + " has an open ring");
        }
      }
      int points =
          shape.outline().geometry().positionCount()
              + shape.coast().geometry().positionCount()
              + shape.landBorders().geometry().positionCount();
      // The outline and the coast repeat the same points, so this is about twice the outline.
      assertTrue(points <= MAX_SHAPE_POINTS, shape.name() + " has " + points + " points");
    }
  }

  @Test
  public void eachCountryShapeGameHasEnoughCountries() {
    List<CountryShape> all = new CountryShapeService(reader).loadShapes();
    assertTrue(all.stream().filter(CountryShapeEligibility.neighbours()).count() >= 20);
    assertTrue(all.stream().filter(CountryShapeEligibility.coastline()).count() >= 20);
    assertTrue(all.stream().filter(CountryShapeEligibility.landBorders()).count() >= 20);
  }

  @Test
  public void everyCountryShapeGameWithNeighboursHasSomeToShow() {
    List<CountryShape> all = new CountryShapeService(reader).loadShapes();
    all.stream()
        .filter(CountryShapeEligibility.neighbours().or(CountryShapeEligibility.landBorders()))
        .forEach(
            shape ->
                assertTrue(
                    shape.neighbours().stream().anyMatch(CountryShape.Neighbour::mainland),
                    shape.name() + " has no mainland neighbours to show"));
  }

  @Test
  void everyCountryShapeHasOneOrTwoOfTheSixContinents() {
    Set<String> continents =
        Set.of("Africa", "Asia", "Europe", "North America", "South America", "Oceania");
    for (CountryShape shape : shapeService.loadShapes()) {
      assertTrue(
          shape.continents().size() >= 1 && shape.continents().size() <= 2,
          shape.name() + " has " + shape.continents());
      assertTrue(
          continents.containsAll(shape.continents()), shape.name() + " " + shape.continents());
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void everyRiverIsOnePiece() {
    for (River river : new RiverLoaderService(reader).loadRivers()) {
      List<List<List<Number>>> raw = river.geoData().geometry().coordinates();
      List<List<double[]>> lines = new java.util.ArrayList<>();
      for (List<List<Number>> line : raw) {
        lines.add(
            line.stream()
                .map(p -> new double[] {p.get(0).doubleValue(), p.get(1).doubleValue()})
                .toList());
      }
      assertEquals(1, pieceCount(lines), river.name() + " is drawn in several pieces");
    }
  }

  private static int pieceCount(List<List<double[]>> lines) {
    int n = lines.size();
    int[] piece = new int[n];
    for (int i = 0; i < n; i++) {
      piece[i] = i;
    }
    for (int i = 0; i < n; i++) {
      for (int j = i + 1; j < n; j++) {
        if (piece[i] != piece[j] && withinGap(lines.get(i), lines.get(j))) {
          int from = piece[j];
          for (int k = 0; k < n; k++) {
            if (piece[k] == from) {
              piece[k] = piece[i];
            }
          }
        }
      }
    }
    return (int) java.util.Arrays.stream(piece).distinct().count();
  }

  private static boolean withinGap(List<double[]> a, List<double[]> b) {
    for (double[] p : a) {
      for (double[] q : b) {
        if (Geo.haversineKm(p, q) <= 25) {
          return true;
        }
      }
    }
    return false;
  }
}
