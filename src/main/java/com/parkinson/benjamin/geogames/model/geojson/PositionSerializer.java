package com.parkinson.benjamin.geogames.model.geojson;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

/**
 * Writes a {@link Position} as {@code [x, y]}, exactly as a list of two Doubles is written, without
 * boxing the two numbers.
 */
public class PositionSerializer extends ValueSerializer<Position> {

  @Override
  public void serialize(Position value, JsonGenerator gen, SerializationContext ctxt) {
    gen.writeStartArray();
    gen.writeNumber(value.x());
    gen.writeNumber(value.y());
    gen.writeEndArray();
  }
}
