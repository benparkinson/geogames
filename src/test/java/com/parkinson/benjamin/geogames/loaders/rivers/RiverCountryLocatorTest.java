package com.parkinson.benjamin.geogames.loaders.rivers;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkinson.benjamin.geogames.model.Country;
import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import com.parkinson.benjamin.geogames.model.geojson.GeoGeometry;
import com.parkinson.benjamin.geogames.model.geojson.GeoProperties;
import java.util.List;
import org.junit.jupiter.api.Test;

class RiverCountryLocatorTest {

  private static List<List<Double>> ring(
      double minLon, double minLat, double maxLon, double maxLat) {
    return List.of(
        List.of(minLon, minLat),
        List.of(maxLon, minLat),
        List.of(maxLon, maxLat),
        List.of(minLon, maxLat),
        List.of(minLon, minLat));
  }

  private static Country square(
      String name, double minLon, double minLat, double maxLon, double maxLat) {
    return country(name, "Polygon", List.of(ring(minLon, minLat, maxLon, maxLat)));
  }

  private static Country country(String name, String type, List<?> coordinates) {
    return new Country(
        name,
        List.of(),
        new GeoData("Feature", new GeoProperties(name), new GeoGeometry(type, coordinates)));
  }

  private static List<List<double[]>> eastWest(double lat, double fromLon, double toLon) {
    return List.of(List.of(new double[] {fromLon, lat}, new double[] {toLon, lat}));
  }

  @Test
  void listsEveryCountryTheRiverCrossesLongestFirst() {
    RiverCountryLocator locator =
        new RiverCountryLocator(
            List.of(
                square("A", 0, -1, 1, 1), // about 111 km of the river
                square("B", 1, -1, 3, 1))); // about 222 km

    RiverCountryLocator.Result result = locator.locate(eastWest(0, 0, 3));

    assertThat(result.countries()).containsExactly("B", "A");
    assertThat(result.unlocatedKm()).isLessThan(1);
  }

  @Test
  void dropsACountryTheRiverOnlyGrazes() {
    // 0.045 degrees is about 5 km.
    RiverCountryLocator locator =
        new RiverCountryLocator(
            List.of(square("A", 0, -1, 2, 1), square("Sliver", 2, -1, 2.045, 1)));

    assertThat(locator.locate(eastWest(0, 0, 2.045)).countries()).containsExactly("A");
  }

  @Test
  void breaksTiesByName() {
    RiverCountryLocator locator =
        new RiverCountryLocator(List.of(square("Zed", 1, -1, 2, 1), square("Alpha", 0, -1, 1, 1)));

    assertThat(locator.locate(eastWest(0, 0, 2)).countries()).containsExactly("Alpha", "Zed");
  }

  @Test
  void doesNotCountTimeInAHole() {
    // The river runs through 2 degrees of land, 2 of hole, then 2 of land again. The hole is
    // filled by another country.
    List<List<List<Double>>> withHole = List.of(ring(0, -1, 6, 1), ring(2, -0.5, 4, 0.5));
    RiverCountryLocator locator =
        new RiverCountryLocator(
            List.of(country("Donut", "Polygon", withHole), square("Lake", 2, -0.5, 4, 0.5)));

    RiverCountryLocator.Result result = locator.locate(eastWest(0, 0, 6));

    assertThat(result.countries()).containsExactly("Donut", "Lake");
    assertThat(result.unlocatedKm()).isLessThan(1);
  }

  @Test
  void aHoleWithNothingInItIsNotACountry() {
    List<List<List<Double>>> withHole = List.of(ring(0, -1, 6, 1), ring(2, -0.5, 4, 0.5));
    RiverCountryLocator locator =
        new RiverCountryLocator(List.of(country("Donut", "Polygon", withHole)));

    RiverCountryLocator.Result result = locator.locate(eastWest(0, 0, 6));

    assertThat(result.countries()).containsExactly("Donut");
    assertThat(result.unlocatedKm()).isBetween(215.0, 230.0);
  }

  @Test
  void countsTheKmOutsideEveryCountry() {
    RiverCountryLocator locator = new RiverCountryLocator(List.of(square("A", 0, -1, 1, 1)));

    RiverCountryLocator.Result result = locator.locate(eastWest(0, 0, 2));

    assertThat(result.countries()).containsExactly("A");
    assertThat(result.unlocatedKm()).isBetween(100.0, 125.0);
  }

  @Test
  void readsMultiPolygons() {
    List<List<List<List<Double>>>> parts =
        List.of(List.of(ring(0, -1, 1, 1)), List.of(ring(2, -1, 3, 1)));
    RiverCountryLocator locator =
        new RiverCountryLocator(List.of(country("Islands", "MultiPolygon", parts)));

    assertThat(locator.locate(eastWest(0, 0, 3)).countries()).containsExactly("Islands");
  }

  @Test
  void ignoresAntarctica() {
    RiverCountryLocator locator =
        new RiverCountryLocator(List.of(square("Antarctica", 0, -1, 3, 1)));

    assertThat(locator.locate(eastWest(0, 0, 3)).countries()).isEmpty();
  }
}
