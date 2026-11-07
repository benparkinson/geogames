package com.parkinson.benjamin.geogames.helper;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.json.JsonMapper;

/**
 * The JSON settings for the geo data files, shared by the app and the offline loaders. Both are
 * safe to share between threads, and need no Spring.
 */
public final class GeoJson {

  private static final ObjectMapper LENIENT_READER =
      JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();

  private static final ObjectWriter PRETTY_WRITER =
      JsonMapper.builder()
          .disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
          .build()
          .writerWithDefaultPrettyPrinter();

  private GeoJson() {}

  /** Reads JSON files that may hold extra properties we don't model, such as raw exports. */
  public static ObjectMapper lenientReader() {
    return LENIENT_READER;
  }

  /** Writes indented JSON with properties in the order the records declare them. */
  public static ObjectWriter prettyWriter() {
    return PRETTY_WRITER;
  }
}
