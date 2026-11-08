package com.parkinson.benjamin.geogames;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class JsonShapeTest {

  private final ObjectMapper mapper = new ObjectMapper();

  private Object shape(String json) {
    return JsonShape.of(mapper.readTree(json));
  }

  @Test
  void describesObjectsAndPrimitives() {
    assertThat(shape("{\"b\":1,\"a\":\"x\",\"c\":true,\"d\":null}"))
        .isEqualTo(Map.of("a", "string", "b", "number", "c", "boolean", "d", "null"));
  }

  @Test
  void describesArraysByTheirMergedElementsAndEmptyOnes() {
    assertThat(shape("[]")).isEqualTo(Map.of("array", "empty"));
    assertThat(shape("[{\"a\":1},{\"b\":\"x\"},{\"a\":2}]"))
        .isEqualTo(Map.of("array", Map.of("a", "number", "b", "string")));
  }

  @Test
  void treatsCoordinatesAsOpaque() {
    assertThat(shape("{\"coordinates\":[[[1,2]]]}"))
        .isEqualTo(Map.of("coordinates", "coordinates"));
  }

  @Test
  void mergesNullAndEmptyAwayAndJoinsDifferentPrimitives() {
    assertThat(JsonShape.merge("empty", "string")).isEqualTo("string");
    assertThat(JsonShape.merge("number", "null")).isEqualTo("number");
    assertThat(JsonShape.merge("string", "number")).isEqualTo("number|string");
    assertThat(JsonShape.merge("number|string", "boolean")).isEqualTo("boolean|number|string");
  }

  @Test
  void refusesToMergeAnObjectWithAPrimitive() {
    assertThatThrownBy(() -> JsonShape.merge(Map.of("a", "string"), "number"))
        .isInstanceOf(IllegalStateException.class);
  }
}
