package com.parkinson.benjamin.geogames.helper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
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

  /** Reads a JSON file into the given type, ignoring properties we don't model. */
  public static <T> T readValue(Path file, TypeReference<T> type) throws IOException {
    try (InputStream in = Files.newInputStream(file)) {
      return LENIENT_READER.readValue(in, type);
    }
  }

  /** Reads a JSON file into a tree, such as a raw Natural Earth export. */
  public static JsonNode readTree(Path file) throws IOException {
    try (InputStream in = Files.newInputStream(file)) {
      return LENIENT_READER.readTree(in);
    }
  }

  /** Reads JSON files that may hold extra properties we don't model, such as raw exports. */
  public static ObjectMapper lenientReader() {
    return LENIENT_READER;
  }

  /** Writes indented JSON with properties in the order the records declare them. */
  public static ObjectWriter prettyWriter() {
    return PRETTY_WRITER;
  }
}
