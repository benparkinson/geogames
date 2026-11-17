package com.parkinson.benjamin.geogames.model.geojson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parkinson.benjamin.geogames.helper.GeoJson;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class CoordinatesJsonTest {

  private static final String POLYGON =
      "{\"type\":\"Polygon\",\"coordinates\":[[[1.5,2.5],[3.5,4.5],[1.5,2.5]]]}";
  private static final String MULTI_POLYGON =
      "{\"type\":\"MultiPolygon\",\"coordinates\":[[[[1.5,2.5],[3.5,4.5]]],[[[5.5,6.5],[7.5,8.5]]]]}";
  private static final String MULTI_LINE_STRING =
      "{\"type\":\"MultiLineString\",\"coordinates\":[[[1.5,2.5],[3.5,4.5]],[[5.5,6.5],[7.5,8.5]]]}";
  private static final String THREE_VALUES =
      "{\"type\":\"MultiLineString\",\"coordinates\":[[[1.5,2.5,100.0],[3.5,4.5,200.0]]]}";
  private static final String INTEGERS =
      "{\"type\":\"MultiLineString\",\"coordinates\":[[[1,2],[3,4],[5000000000,6]]]}";
  private static final String MIXED =
      "{\"type\":\"MultiLineString\",\"coordinates\":[[[1,2.5],[3.5,4]]]}";
  private static final String EMPTY = "{\"type\":\"MultiLineString\",\"coordinates\":[]}";

  private final ObjectMapper reader = GeoJson.lenientReader();
  private final ObjectMapper plain = JsonMapper.builder().build();

  private GeoGeometry read(String json) {
    return reader.readValue(json, GeoGeometry.class);
  }

  /** What the geometry would be had the coordinates been read as plain nested lists. */
  private GeoGeometry plainGeometry(String json) {
    GeoGeometry geometry = read(json);
    return new GeoGeometry(geometry.type(), plain.readValue(coordinatesOf(json), List.class));
  }

  private String coordinatesOf(String json) {
    return plain.readTree(json).get("coordinates").toString();
  }

  @ParameterizedTest
  @ValueSource(strings = {POLYGON, MULTI_POLYGON, MULTI_LINE_STRING})
  void innermostPairsOfDoublesArePositions(String json) {
    GeoGeometry geometry = read(json);

    List<List<Number>> flattened = geometry.flattenedCoordinates();

    assertThat(flattened).isNotEmpty().allSatisfy(p -> assertThat(p).isInstanceOf(Position.class));
    assertThat(flattened).isEqualTo(plainGeometry(json).flattenedCoordinates());
  }

  @Test
  void aMultiPolygonKeepsItsValues() {
    assertThat(read(MULTI_POLYGON).flattenedCoordinates())
        .containsExactly(
            List.of(1.5, 2.5), List.of(3.5, 4.5), List.of(5.5, 6.5), List.of(7.5, 8.5));
  }

  @Test
  void aPositionWithThreeValuesIsAPlainList() {
    List<List<Number>> flattened = read(THREE_VALUES).flattenedCoordinates();

    assertThat(flattened).containsExactly(List.of(1.5, 2.5, 100.0), List.of(3.5, 4.5, 200.0));
    assertThat(flattened).allSatisfy(p -> assertThat(p).isExactlyInstanceOf(ArrayList.class));
  }

  @Test
  void aPositionOfIntegersIsAPlainListOfTheSameNumberTypes() {
    List<List<Number>> flattened = read(INTEGERS).flattenedCoordinates();

    assertThat(flattened).allSatisfy(p -> assertThat(p).isExactlyInstanceOf(ArrayList.class));
    assertThat(flattened.get(0)).containsExactly(1, 2);
    assertThat(flattened.get(2)).containsExactly(5000000000L, 6);
    assertThat(flattened).isEqualTo(plainGeometry(INTEGERS).flattenedCoordinates());
  }

  @Test
  void aPositionOfAnIntegerAndADoubleIsAPlainList() {
    List<List<Number>> flattened = read(MIXED).flattenedCoordinates();

    assertThat(flattened).allSatisfy(p -> assertThat(p).isExactlyInstanceOf(ArrayList.class));
    assertThat(flattened).isEqualTo(plainGeometry(MIXED).flattenedCoordinates());
    assertThat(flattened.get(0).get(0)).isInstanceOf(Integer.class);
  }

  @Test
  void emptyCoordinatesAreAnEmptyMutableList() {
    GeoGeometry geometry = read(EMPTY);

    assertThat(geometry.coordinates()).isExactlyInstanceOf(ArrayList.class);
    assertThat(geometry.coordinates()).isEmpty();
  }

  @Test
  void aStringInTheCoordinatesIsAnError() {
    assertThatThrownBy(() -> read("{\"type\":\"Polygon\",\"coordinates\":[[[1.5,\"x\"]]]}"))
        .isInstanceOf(RuntimeException.class);
  }

  @Test
  void aNullInTheCoordinatesIsAnError() {
    assertThatThrownBy(() -> read("{\"type\":\"Polygon\",\"coordinates\":[[[1.5,null]]]}"))
        .isInstanceOf(RuntimeException.class);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {POLYGON, MULTI_POLYGON, MULTI_LINE_STRING, THREE_VALUES, INTEGERS, MIXED, EMPTY})
  void writingWhatWasReadGivesTheSameJsonAsPlainLists(String json) {
    GeoGeometry read = read(json);
    GeoGeometry plainGeometry = plainGeometry(json);

    assertThat(plain.writeValueAsString(read)).isEqualTo(plain.writeValueAsString(plainGeometry));
    assertThat(GeoJson.prettyWriter().writeValueAsString(read))
        .isEqualTo(GeoJson.prettyWriter().writeValueAsString(plainGeometry));
  }

  @Test
  void aPositionIsWrittenWithThePrettyPrintersArraySpacing() {
    String written = GeoJson.prettyWriter().writeValueAsString(new Position(107.783, 54.2938));

    assertThat(written).isEqualTo("[ 107.783, 54.2938 ]");
  }
}
