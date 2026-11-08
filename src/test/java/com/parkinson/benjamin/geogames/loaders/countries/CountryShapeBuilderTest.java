package com.parkinson.benjamin.geogames.loaders.countries;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parkinson.benjamin.geogames.model.Country;
import com.parkinson.benjamin.geogames.model.CountryShape;
import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import com.parkinson.benjamin.geogames.model.geojson.GeoGeometry;
import com.parkinson.benjamin.geogames.model.geojson.GeoProperties;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CountryShapeBuilderTest {

  /** A closed ring from points; the first point is repeated at the end. */
  private static List<List<Double>> ring(double[]... points) {
    List<List<Double>> ring = new ArrayList<>();
    for (double[] p : points) {
      ring.add(List.of(p[0], p[1]));
    }
    ring.add(ring.get(0));
    return ring;
  }

  private static double[] p(double lon, double lat) {
    return new double[] {lon, lat};
  }

  private static List<List<Double>> square(
      double minLon, double minLat, double maxLon, double maxLat) {
    return ring(p(minLon, minLat), p(maxLon, minLat), p(maxLon, maxLat), p(minLon, maxLat));
  }

  /** A Polygon: the outer ring, then any holes. */
  @SafeVarargs
  private static Country polygon(String name, List<List<Double>>... rings) {
    return country(name, "Polygon", List.of(rings));
  }

  /** A MultiPolygon where each part is just an outer ring. */
  @SafeVarargs
  private static Country multi(String name, List<List<Double>>... outers) {
    List<Object> parts = new ArrayList<>();
    for (List<List<Double>> outer : outers) {
      parts.add(List.of(outer));
    }
    return country(name, "MultiPolygon", parts);
  }

  private static Country country(String name, String type, List<?> coordinates) {
    return new Country(
        name,
        List.of(name + " alias"),
        new GeoData("Feature", new GeoProperties(name), new GeoGeometry(type, coordinates)));
  }

  private static CountryShape find(List<CountryShape> shapes, String name) {
    return shapes.stream().filter(s -> s.name().equals(name)).findFirst().orElseThrow();
  }

  private static List<?> lines(GeoData data) {
    return data.geometry().coordinates();
  }

  private static Set<String> pointSet(Object coordinates) {
    Set<String> points = new HashSet<>();
    collect(coordinates, points);
    return points;
  }

  private static void collect(Object o, Set<String> points) {
    List<?> list = (List<?>) o;
    if (!list.isEmpty() && list.get(0) instanceof Number) {
      points.add(list.get(0) + "," + list.get(1));
      return;
    }
    for (Object child : list) {
      collect(child, points);
    }
  }

  @Test
  void sharedEdgeIsALandBorderForBothCountries() {
    Country a = polygon("A", ring(p(0, 0), p(1, 0), p(1, 0.5), p(1, 1), p(0, 1)));
    Country b = polygon("B", ring(p(1, 0), p(2, 0), p(2, 1), p(1, 1), p(1, 0.5)));

    List<CountryShape> shapes = CountryShapeBuilder.build(List.of(a, b));

    CountryShape shapeA = find(shapes, "A");
    CountryShape shapeB = find(shapes, "B");
    assertThat(shapeA.neighbours()).hasSize(1);
    assertThat(shapeA.neighbours().get(0).name()).isEqualTo("B");
    assertThat(shapeA.neighbours().get(0).km()).isEqualTo(111);
    assertThat(shapeA.neighbours().get(0).mainland()).isTrue();
    assertThat(shapeB.neighbours().get(0).name()).isEqualTo("A");
    assertThat(shapeA.landBorderKm()).isEqualTo(111);
    assertThat(shapeA.coastKm()).isBetween(333.0, 334.0);
    assertThat(lines(shapeA.landBorders())).hasSize(1);
    assertThat(lines(shapeA.coast())).hasSize(1);
    assertThat(shapeA.additionalNames()).containsExactly("A alias");
    // The border is straight, so the middle vertex is simplified away.
    assertThat(pointSet(shapeA.landBorders().geometry().coordinates())).doesNotContain("1.0,0.5");
  }

  @Test
  void seamEdgesAreNeitherCoastNorBorderAndAreNotOutputAsLines() {
    Country fiji = polygon("Fiji", square(179, 0, 180, 1));

    CountryShape shape = CountryShapeBuilder.build(List.of(fiji)).get(0);

    assertThat(shape.coastKm()).isBetween(333.0, 334.0); // three sides, not four
    assertThat(shape.landBorderKm()).isZero();
    assertThat(lines(shape.coast())).hasSize(1);
    List<?> coast = (List<?>) lines(shape.coast()).get(0);
    assertThat(coast).hasSize(4); // the three coast sides; the seam edge on 180 is not a line
    // The outline is still a closed ring.
    List<?> ring = (List<?>) ((List<?>) shape.outline().geometry().coordinates()).get(0);
    assertThat(ring.get(0)).isEqualTo(ring.get(ring.size() - 1));
    assertThat(pointSet(ring)).contains("180.0,0.0", "180.0,1.0");
  }

  @Test
  void anEnclaveSharesItsRingWithTheHoleOfItsHost() {
    // The hole runs clockwise and the enclave counter-clockwise, with different start points.
    List<List<Double>> hole = ring(p(4, 4), p(4, 5), p(4, 6), p(5, 6), p(6, 6), p(6, 4));
    List<List<Double>> inner = ring(p(5, 6), p(4, 6), p(4, 5), p(4, 4), p(6, 4), p(6, 6));
    Country outer = polygon("Outer", square(0, 0, 10, 10), hole);
    Country enclave = polygon("Inner", inner);

    List<CountryShape> shapes = CountryShapeBuilder.build(List.of(outer, enclave));

    CountryShape shapeInner = find(shapes, "Inner");
    CountryShape shapeOuter = find(shapes, "Outer");
    assertThat(shapeInner.coastKm()).isZero();
    assertThat(shapeInner.neighbours()).extracting("name").containsExactly("Outer");
    assertThat(shapeOuter.neighbours()).extracting("name").containsExactly("Inner");

    List<?> outerRings = (List<?>) shapeOuter.outline().geometry().coordinates();
    assertThat(outerRings).hasSize(2);
    Set<String> holePoints = pointSet(outerRings.get(1));
    Set<String> innerPoints =
        pointSet(((List<?>) shapeInner.outline().geometry().coordinates()).get(0));
    assertThat(holePoints).isEqualTo(innerPoints);
  }

  @Test
  void aSharedBorderIsSimplifiedIdenticallyForBothCountries() {
    List<double[]> border = new ArrayList<>();
    for (int i = 1; i < 50; i++) {
      border.add(p(1 + (i % 10 == 0 ? 0.05 : 0.001 * (i % 2)), i / 50.0));
    }
    List<double[]> aPoints = new ArrayList<>(List.of(p(0, 0), p(1, 0)));
    aPoints.addAll(border);
    aPoints.addAll(List.of(p(1, 1), p(0, 1)));

    List<double[]> reversed = new ArrayList<>(border);
    Collections.reverse(reversed);
    List<double[]> bPoints = new ArrayList<>(List.of(p(1, 0), p(2, 0), p(2, 1), p(1, 1)));
    bPoints.addAll(reversed);

    List<CountryShape> shapes =
        CountryShapeBuilder.build(
            List.of(
                polygon("A", ring(aPoints.toArray(new double[0][]))),
                polygon("B", ring(bPoints.toArray(new double[0][])))));

    List<?> lineA = (List<?>) lines(find(shapes, "A").landBorders()).get(0);
    List<?> lineB = new ArrayList<>((List<?>) lines(find(shapes, "B").landBorders()).get(0));
    Collections.reverse(lineB);
    assertThat(lineA).isEqualTo(lineB);
    assertThat(lineA.size()).isLessThan(border.size() + 2).isGreaterThan(2);
  }

  @Test
  void tinyCoastIslandsAreDroppedButMicroStatesKeepTheirOnlyRing() {
    Country big =
        multi(
            "Big",
            square(0, 0, 1, 1),
            square(5, 5, 5.01, 5.01), // about 4 km round: dropped
            square(6, 6, 6.2, 6.2)); // about 90 km round: kept
    Country nauru = polygon("Nauru", square(166.9, -0.55, 166.95, -0.5));

    List<CountryShape> shapes = CountryShapeBuilder.build(List.of(big, nauru));

    List<?> parts = (List<?>) find(shapes, "Big").outline().geometry().coordinates();
    assertThat(parts).hasSize(2);
    assertThat(find(shapes, "Big").outline().geometry().type()).isEqualTo("MultiPolygon");
    assertThat(find(shapes, "Nauru").outline().geometry().type()).isEqualTo("Polygon");
    assertThat(lines(find(shapes, "Nauru").coast())).hasSize(1);
  }

  @Test
  void focusIsTheLargestPartAndOverseasNeighboursAreNotMainland() {
    Country france = multi("France", square(0, 0, 2, 2), square(10, 10, 10.5, 10.5));
    Country mainlandNeighbour = polygon("Spain", square(-1, 0, 0, 2));
    Country overseasNeighbour = polygon("Suriname", square(10.5, 10, 11, 10.5));

    List<CountryShape> shapes =
        CountryShapeBuilder.build(List.of(france, mainlandNeighbour, overseasNeighbour));

    CountryShape shape = find(shapes, "France");
    assertThat(shape.focus().west()).isEqualTo(0);
    assertThat(shape.focus().south()).isEqualTo(0);
    assertThat(shape.focus().east()).isEqualTo(2);
    assertThat(shape.focus().north()).isEqualTo(2);
    assertThat(shape.neighbours())
        .extracting("name", "mainland")
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple("Spain", true),
            org.assertj.core.groups.Tuple.tuple("Suriname", false));
  }

  @Test
  void neighboursAreOrderedLongestBorderFirstThenByName() {
    Country middle = polygon("Middle", ring(p(0, 0), p(3, 0), p(3, 1), p(3, 2), p(3, 3), p(0, 3)));
    Country longBorder = polygon("Long", square(-1, 0, 0, 3));
    Country shortB = polygon("Bshort", square(3, 0, 4, 1));
    Country shortA = polygon("Ashort", square(3, 1, 4, 2));

    CountryShape shape =
        find(CountryShapeBuilder.build(List.of(middle, longBorder, shortB, shortA)), "Middle");

    assertThat(shape.neighbours()).extracting("name").containsExactly("Long", "Ashort", "Bshort");
  }

  @Test
  void antarcticaIsSkipped() {
    List<CountryShape> shapes =
        CountryShapeBuilder.build(
            List.of(
                polygon("Antarctica", square(0, -80, 10, -70)), polygon("A", square(0, 0, 1, 1))));

    assertThat(shapes).extracting(CountryShape::name).containsExactly("A");
  }

  @Test
  void threeCountriesSharingAnEdgeIsADataError() {
    Country a = polygon("A", square(0, 0, 1, 1));
    Country b = polygon("B", square(1, 0, 2, 1));
    Country c = polygon("C", square(1, 0, 3, 1));

    assertThatThrownBy(() -> CountryShapeBuilder.build(List.of(a, b, c)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("A")
        .hasMessageContaining("B")
        .hasMessageContaining("C");
  }

  @Test
  void verticesThatDifferByLessThanAMetreStillMatch() {
    Country a = polygon("A", square(0, 0, 1, 1));
    Country b = polygon("B", square(1.0000001, 0.0000001, 2, 1.0000001));

    CountryShape shape = find(CountryShapeBuilder.build(List.of(a, b)), "A");

    assertThat(shape.neighbours()).extracting("name").containsExactly("B");
  }
}
