package com.parkinson.benjamin.geogames.model.geojson;

import java.util.ArrayList;
import java.util.List;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/**
 * Reads the nested arrays of numbers in {@code coordinates}, to any depth. The values are what
 * Jackson's untyped reading gives (a {@code Double} for a float, an {@code Integer} or {@code Long}
 * for an integer, a mutable {@code ArrayList} for an array), except that an array of exactly two
 * Doubles becomes a {@link Position}. Positions with three values, or written as integers, stay
 * plain lists.
 */
public class CoordinatesDeserializer extends ValueDeserializer<List<?>> {

  @Override
  public List<?> deserialize(JsonParser p, DeserializationContext ctxt) {
    if (!p.isExpectedStartArrayToken()) {
      return (List<?>) ctxt.handleUnexpectedToken(List.class, p);
    }
    return readArray(p, ctxt);
  }

  /** Reads an array whose START_ARRAY is the current token. */
  private static List<?> readArray(JsonParser p, DeserializationContext ctxt) {
    ArrayList<Object> values = new ArrayList<>(2);
    for (JsonToken token = p.nextToken(); token != JsonToken.END_ARRAY; token = p.nextToken()) {
      values.add(readValue(token, p, ctxt));
    }
    if (values.size() == 2
        && values.get(0) instanceof Double x
        && values.get(1) instanceof Double y) {
      return new Position(x, y);
    }
    values.trimToSize();
    return values;
  }

  private static Object readValue(JsonToken token, JsonParser p, DeserializationContext ctxt) {
    return switch (token) {
      case START_ARRAY -> readArray(p, ctxt);
      case VALUE_NUMBER_FLOAT -> p.getDoubleValue();
      case VALUE_NUMBER_INT -> p.getNumberValue();
      default -> ctxt.handleUnexpectedToken(Number.class, p);
    };
  }
}
