package com.parkinson.benjamin.geogames;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import tools.jackson.databind.JsonNode;

/**
 * Describes the shape of a JSON value as another JSON value, for the contract between the backend
 * and the frontend. The frontend has the same rules in {@code client/src/test/shape.ts}; change
 * them together.
 *
 * <ul>
 *   <li>An object becomes an object with the same keys, each mapped to its child's shape.
 *   <li>An array becomes {@code {"array": <merged shape of its elements>}}, and an empty array
 *       {@code {"array": "empty"}}.
 *   <li>Primitives become {@code "string"}, {@code "number"} or {@code "boolean"}; null becomes
 *       {@code "null"}.
 *   <li>The value of any key named {@code coordinates} becomes {@code "coordinates"}, because
 *       GeoJSON nesting depth varies between geometry types.
 *   <li>Merging two object shapes takes the union of their keys. {@code "empty"} and {@code
 *       "null"} give way to the other shape, so a field that is sometimes null is described by its
 *       real type. Two different primitives join with {@code |}, sorted ({@code "number|string"}).
 * </ul>
 *
 * <p>Shapes are plain {@code Map} / {@code String} values (keys sorted), so the JSON mapper can
 * write them directly.
 */
final class JsonShape {

  private static final String EMPTY = "empty";
  private static final String NULL = "null";

  private JsonShape() {}

  static Object of(JsonNode node) {
    if (node.isObject()) {
      Map<String, Object> shape = new TreeMap<>();
      for (Map.Entry<String, JsonNode> entry : node.properties()) {
        shape.put(
            entry.getKey(),
            entry.getKey().equals("coordinates") ? "coordinates" : of(entry.getValue()));
      }
      return shape;
    }
    if (node.isArray()) {
      Object merged = EMPTY;
      for (JsonNode element : node) {
        merged = merge(merged, of(element));
      }
      return Map.of("array", merged);
    }
    if (node.isNull()) {
      return NULL;
    }
    if (node.isBoolean()) {
      return "boolean";
    }
    return node.isNumber() ? "number" : "string";
  }

  @SuppressWarnings("unchecked")
  static Object merge(Object a, Object b) {
    if (a.equals(b) || b.equals(EMPTY) || b.equals(NULL)) {
      return a;
    }
    if (a.equals(EMPTY) || a.equals(NULL)) {
      return b;
    }
    if (a instanceof Map<?, ?> left && b instanceof Map<?, ?> right) {
      Map<String, Object> union = new TreeMap<>((Map<String, Object>) left);
      ((Map<String, Object>) right)
          .forEach((key, value) -> union.merge(key, value, JsonShape::merge));
      return union;
    }
    if (a instanceof String left && b instanceof String right) {
      TreeSet<String> parts = new TreeSet<>(java.util.List.of(left.split("\\|")));
      parts.addAll(java.util.List.of(right.split("\\|")));
      return String.join("|", new ArrayList<>(parts));
    }
    throw new IllegalStateException("Cannot merge the shapes " + a + " and " + b);
  }
}
