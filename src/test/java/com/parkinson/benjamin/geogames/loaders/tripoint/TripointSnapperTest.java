package com.parkinson.benjamin.geogames.loaders.tripoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.parkinson.benjamin.geogames.model.Coordinate;
import com.parkinson.benjamin.geogames.model.Country;
import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import com.parkinson.benjamin.geogames.model.geojson.GeoGeometry;
import com.parkinson.benjamin.geogames.model.geojson.GeoProperties;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public class TripointSnapperTest {

  private final TripointSnapper snapper = new TripointSnapper();

  @Test
  public void snapsToSharedCorner() {
    // three squares all containing the corner (10.0, 20.0) as a vertex
    List<Country> countries =
        List.of(
            square("A", 10.0, 20.0, 1, 1),
            square("B", 10.0, 20.0, -1, 1),
            square("C", 10.0, 20.0, 1, -1));

    Optional<Coordinate> snapped = snapper.snap(new Coordinate(20.01, 10.01), countries);

    assertTrue(snapped.isPresent());
    assertEquals(20.0, snapped.get().latitude(), 1e-9);
    assertEquals(10.0, snapped.get().longitude(), 1e-9);
  }

  @Test
  public void returnsEmptyWithoutSharedVertex() {
    List<Country> countries =
        List.of(
            square("A", 10.0, 20.0, 1, 1),
            square("B", 10.0, 20.0, -1, 1),
            square("C", 10.2, 20.2, 1, -1));

    assertTrue(snapper.snap(new Coordinate(20.0, 10.0), countries).isEmpty());
  }

  @Test
  public void returnsEmptyWhenJunctionTooFarAway() {
    // junction at lat 20.4, about 44 km from the original coordinate, still in the search window
    List<Country> countries =
        List.of(
            square("A", 10.0, 20.4, 1, 1),
            square("B", 10.0, 20.4, -1, 1),
            square("C", 10.0, 20.4, 1, -1));

    assertTrue(snapper.snap(new Coordinate(20.0, 10.0), countries).isEmpty());
  }

  @Test
  public void snapsAJunctionJustInsideThirtyKilometres() {
    // junction at lat 20.25, about 27.6 km from the original coordinate
    List<Country> countries =
        List.of(
            square("A", 10.0, 20.25, 1, 1),
            square("B", 10.0, 20.25, -1, 1),
            square("C", 10.0, 20.25, 1, -1));

    Optional<Coordinate> snapped = snapper.snap(new Coordinate(20.0, 10.0), countries);

    assertTrue(snapped.isPresent());
    assertEquals(20.25, snapped.get().latitude(), 1e-9);
  }

  @Test
  public void needsExactlyThreeCountries() {
    List<Country> two = List.of(square("A", 10.0, 20.0, 1, 1), square("B", 10.0, 20.0, -1, 1));
    List<Country> four =
        List.of(
            square("A", 10.0, 20.0, 1, 1),
            square("B", 10.0, 20.0, -1, 1),
            square("C", 10.0, 20.0, 1, -1),
            square("D", 10.0, 20.0, -1, -1));

    assertTrue(snapper.snap(new Coordinate(20.0, 10.0), two).isEmpty());
    assertTrue(snapper.snap(new Coordinate(20.0, 10.0), four).isEmpty());
  }

  @Test
  public void returnsEmptyWhenACountryHasNoVertexInTheSearchWindow() {
    List<Country> countries =
        List.of(
            square("A", 10.0, 20.0, 1, 1),
            square("B", 10.0, 20.0, -1, 1),
            square("Far", 50.0, 50.0, 1, 1));

    assertTrue(snapper.snap(new Coordinate(20.0, 10.0), countries).isEmpty());
  }

  /** A square with one corner at (lon, lat) extending by dLon/dLat degrees. */
  private Country square(String name, double lon, double lat, int dLon, int dLat) {
    List<List<List<Double>>> ring =
        List.of(
            List.of(
                List.of(lon, lat),
                List.of(lon + dLon * 0.1, lat),
                List.of(lon + dLon * 0.1, lat + dLat * 0.1),
                List.of(lon, lat + dLat * 0.1),
                List.of(lon, lat)));
    GeoData geoData =
        new GeoData("Feature", new GeoProperties(name), new GeoGeometry("Polygon", ring));
    return new Country(name, List.of(), geoData);
  }
}
