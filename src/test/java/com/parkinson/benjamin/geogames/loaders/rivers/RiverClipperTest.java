package com.parkinson.benjamin.geogames.loaders.rivers;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkinson.benjamin.geogames.loaders.features.CountryLocator;
import com.parkinson.benjamin.geogames.loaders.rivers.RiverClipper.Run;
import com.parkinson.benjamin.geogames.model.Country;
import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import com.parkinson.benjamin.geogames.model.geojson.GeoGeometry;
import com.parkinson.benjamin.geogames.model.geojson.GeoProperties;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RiverClipperTest {

  private static Country square(
      String name, double minLon, double minLat, double maxLon, double maxLat) {
    List<List<Double>> ring =
        List.of(
            List.of(minLon, minLat),
            List.of(maxLon, minLat),
            List.of(maxLon, maxLat),
            List.of(minLon, maxLat),
            List.of(minLon, minLat));
    return new Country(
        name,
        List.of(),
        new GeoData("Feature", new GeoProperties(name), new GeoGeometry("Polygon", List.of(ring))));
  }

  // A is the west half of 0..10, B the east half; C is far away.
  private final RiverClipper clipper =
      new RiverClipper(
          new CountryLocator(
              List.of(
                  square("A", 0, 0, 5, 10), square("B", 5, 0, 10, 10), square("C", 20, 0, 30, 10)),
              5));

  private static List<double[]> line(double... lonLat) {
    return java.util.stream.IntStream.range(0, lonLat.length / 2)
        .mapToObj(i -> new double[] {lonLat[2 * i], lonLat[2 * i + 1]})
        .toList();
  }

  private static List<Double> lons(Run run) {
    return run.points().stream().map(p -> p[0]).toList();
  }

  @Test
  void cutsALineAtTheBorderBetweenTwoCountries() {
    Map<String, List<Run>> runs =
        clipper.clip(3, 7, null, line(1, 5, 2, 5, 3, 5, 4, 5, 7, 5, 8, 5));

    assertThat(runs).containsOnlyKeys("A", "B");
    assertThat(runs.get("A")).hasSize(1);
    assertThat(lons(runs.get("A").get(0))).containsExactly(1.0, 2.0, 3.0, 4.0);
    assertThat(lons(runs.get("B").get(0))).containsExactly(4.0, 7.0, 8.0);
  }

  @Test
  void aRunKeepsTheRiversScalerankAndNumber() {
    Run run = clipper.clip(4, 99, null, line(1, 5, 2, 5)).get("A").get(0);

    assertThat(run.scalerank()).isEqualTo(4);
    assertThat(run.rivernum()).isEqualTo(99);
  }

  @Test
  void aRiverAlongABorderBelongsToBothCountries() {
    Map<String, List<Run>> runs = clipper.clip(5, 1, null, line(5, 1, 5, 3, 5, 6, 5, 9));

    assertThat(runs).containsOnlyKeys("A", "B");
    assertThat(runs.get("A").get(0).points()).hasSize(4);
    assertThat(runs.get("B").get(0).points()).hasSize(4);
  }

  @Test
  void aLineThatLeavesACountryAndComesBackMakesTwoRuns() {
    // Out of A into open sea and back.
    Map<String, List<Run>> runs =
        clipper.clip(5, 1, null, line(1, 5, 2, 5, 3, 5, -8, 5, -7, 5, 3.5, 5, 4, 5));

    assertThat(runs).containsOnlyKeys("A");
    assertThat(runs.get("A")).hasSize(2);
    assertThat(lons(runs.get("A").get(0))).containsExactly(1.0, 2.0, 3.0);
    assertThat(lons(runs.get("A").get(1))).containsExactly(3.5, 4.0);
  }

  @Test
  void aLineInNoCountryHasNoRuns() {
    assertThat(clipper.clip(5, 1, null, line(12, 5, 13, 5, 14, 5))).isEmpty();
  }

  @Test
  void aLineWithOnePointHasNoSegmentsAndNoRuns() {
    assertThat(clipper.clip(5, 1, null, line(1, 5))).isEmpty();
  }

  @Test
  void aSegmentJustBesideACountryStillBelongsToIt() {
    // 0.02 degrees west of A, inside the 0.03 look-around: a river that runs on the wrong side of
    // a border drawn slightly differently.
    Map<String, List<Run>> runs = clipper.clip(5, 1, null, line(-0.02, 2, -0.02, 4));

    assertThat(runs).containsOnlyKeys("A");
  }

  @Test
  void countriesOfASegmentAreSorted() {
    assertThat(clipper.countriesOf(new double[] {5, 1}, new double[] {5, 3}))
        .containsExactly("A", "B");
  }
}
