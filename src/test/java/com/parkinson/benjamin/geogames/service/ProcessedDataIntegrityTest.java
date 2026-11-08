package com.parkinson.benjamin.geogames.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.parkinson.benjamin.geogames.helper.CountryShapeEligibility;
import com.parkinson.benjamin.geogames.loaders.countries.CountryShapeBuilder;
import com.parkinson.benjamin.geogames.loaders.features.AliasBuilder;
import com.parkinson.benjamin.geogames.model.Country;
import com.parkinson.benjamin.geogames.model.CountryShape;
import com.parkinson.benjamin.geogames.model.Lake;
import com.parkinson.benjamin.geogames.model.PlaceName;
import com.parkinson.benjamin.geogames.model.River;
import com.parkinson.benjamin.geogames.model.TripointDefinition;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

public class ProcessedDataIntegrityTest {

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
  private final CountryLoaderService countryLoaderService = new CountryLoaderService(reader);

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
  public void everyTripointCountryLoads() throws IOException {
    for (TripointDefinition definition : tripointLoaderService.loadTripointDefinitions()) {
      for (String name : definition.countryNames()) {
        Country country = countryLoaderService.loadCountryByName(name);
        assertEquals(name, country.name());
        assertNotNull(country.geoData().geometry(), "no geometry for " + name);
      }
    }
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
        assertEquals(name, countryLoaderService.loadCountryByName(name).name());
      }
    }
  }

  @Test
  public void enoughRiversCrossThreeCountriesForTheCountriesGame() {
    long eligible =
        new RiverLoaderService(reader)
            .loadRivers().stream().filter(river -> river.countries().size() >= 3).count();

    assertTrue(eligible >= 10, "only " + eligible + " rivers cross three or more countries");
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

  private List<Lake> loadLakes() {
    return reader.readAll("classpath*:/processed/lakes/*.json", Lake.class);
  }

  @Test
  public void lakesHaveContinentsCountriesAndClosedRings() {
    List<Lake> lakes = loadLakes();

    assertTrue(lakes.size() >= 20, "only " + lakes.size() + " lakes");
    assertEquals(lakes.size(), lakes.stream().map(Lake::id).distinct().count());
    for (Lake lake : lakes) {
      assertNotNull(lake.continent(), "no continent for " + lake.name());
      assertFalse(lake.countries().isEmpty(), "no countries for " + lake.name());
      for (String name : lake.countries()) {
        assertEquals(name, countryLoaderService.loadCountryByName(name).name());
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
        countryLoaderService.loadCountryNames().stream()
            .filter(c -> !"Antarctica".equals(c.name()))
            .toList();

    List<PlaceName> shapeNames =
        shapes.loadShapes().stream()
            .map(s -> new PlaceName(s.name(), s.additionalNames()))
            .sorted(Comparator.comparing(PlaceName::name))
            .toList();

    // Fails if CountryLoaderMain was re-run without CountryShapeMain.
    assertEquals(countries, shapeNames);
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
          CountryShapeBuilder.pointCount(shape.outline())
              + CountryShapeBuilder.pointCount(shape.coast())
              + CountryShapeBuilder.pointCount(shape.landBorders());
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
}
