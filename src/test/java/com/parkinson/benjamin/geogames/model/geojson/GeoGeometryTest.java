package com.parkinson.benjamin.geogames.model.geojson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public class GeoGeometryTest {

  private final ObjectMapper mapper = JsonMapper.builder().build();

  @Test
  public void polygonCoordinates() {
    List<List<List<Double>>> coordinates =
        List.of(List.of(List.of(12.34, 56.78), List.of(78.9, 12.34)));
    GeoGeometry geometry = new GeoGeometry("Polygon", coordinates);

    List<List<Number>> flattenedCoordinates = geometry.flattenedCoordinates();

    assertThat(flattenedCoordinates).hasSize(2);
    assertThat(flattenedCoordinates).containsOnly(List.of(12.34, 56.78), List.of(78.9, 12.34));
  }

  @Test
  public void multiLineStringCoordinates() {
    List<List<List<Double>>> coordinates =
        List.of(List.of(List.of(12.34, 56.78), List.of(78.9, 12.34)));
    GeoGeometry geometry = new GeoGeometry("MultiLineString", coordinates);

    List<List<Number>> flattenedCoordinates = geometry.flattenedCoordinates();

    assertThat(flattenedCoordinates).hasSize(2);
    assertThat(flattenedCoordinates).containsOnly(List.of(12.34, 56.78), List.of(78.9, 12.34));
  }

  @Test
  public void multiPolygonCoordinates() {
    List<List<List<List<Double>>>> coordinates =
        List.of(List.of(List.of(List.of(12.34, 56.78), List.of(78.9, 12.34))));
    GeoGeometry geometry = new GeoGeometry("MultiPolygon", coordinates);

    List<List<Number>> flattenedCoordinates = geometry.flattenedCoordinates();

    assertThat(flattenedCoordinates).hasSize(2);
    assertThat(flattenedCoordinates).containsOnly(List.of(12.34, 56.78), List.of(78.9, 12.34));
  }

  @ParameterizedTest
  @ValueSource(strings = {"Point", "LineString", "Feature", "polygon"})
  public void rejectsUnsupportedTypes(String type) {
    assertThatThrownBy(() -> new GeoGeometry(type, List.of()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @ParameterizedTest
  @ValueSource(strings = {"Polygon", "MultiPolygon", "MultiLineString"})
  public void emptyCoordinatesFlattenToEmpty(String type) {
    assertThat(new GeoGeometry(type, List.of()).flattenedCoordinates()).isEmpty();
  }

  @Test
  public void deserialisesPolygonFromJson() {
    GeoGeometry geometry =
        mapper.readValue(
            "{\"type\":\"Polygon\",\"coordinates\":[[[1.5,2.5],[3,4]]]}", GeoGeometry.class);

    assertThat(geometry.type()).isEqualTo("Polygon");
    assertThat(geometry.flattenedCoordinates()).hasSize(2);
    assertThat(geometry.flattenedCoordinates().get(0).get(0).doubleValue()).isEqualTo(1.5);
    assertThat(geometry.flattenedCoordinates().get(1).get(1).doubleValue()).isEqualTo(4.0);
  }

  @Test
  public void deserialisesMultiPolygonFromJson() {
    GeoGeometry geometry =
        mapper.readValue(
            "{\"type\":\"MultiPolygon\",\"coordinates\":[[[[1,2],[3,4]]],[[[5,6]]]]}",
            GeoGeometry.class);

    assertThat(geometry.flattenedCoordinates()).hasSize(3);
    assertThat(geometry.flattenedCoordinates().get(2).get(0).doubleValue()).isEqualTo(5.0);
  }

  @Test
  public void visitsEveryPositionOfAMultiPolygon() {
    GeoGeometry geometry =
        new GeoGeometry(
            "MultiPolygon",
            List.of(
                List.of(List.of(List.of(1.0, 2.0), List.of(3.0, 4.0))),
                List.of(List.of(List.of(5.0, 6.0)), List.of())));
    List<String> seen = new java.util.ArrayList<>();

    geometry.forEachPosition((lon, lat) -> seen.add(lon + "," + lat));

    assertThat(seen).containsExactly("1.0,2.0", "3.0,4.0", "5.0,6.0");
    assertThat(geometry.positionCount()).isEqualTo(3);
  }

  @Test
  public void anEmptyGeometryHasNoPositionsAndAddsNoJsonField() {
    GeoGeometry geometry = new GeoGeometry("MultiLineString", List.of());

    assertThat(geometry.positionCount()).isZero();
    assertThat(mapper.writeValueAsString(geometry))
        .isEqualTo("{\"type\":\"MultiLineString\",\"coordinates\":[]}");
  }
}
