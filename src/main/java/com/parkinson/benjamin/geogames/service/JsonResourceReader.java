package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.helper.GeoJson;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Reads JSON files from the classpath. A file that is missing or won't parse is a packaging bug,
 * not something a caller can recover from, so every failure is an {@link UncheckedIOException}
 * whose message names the file.
 */
@Component
public class JsonResourceReader {

  private final ObjectMapper objectMapper = GeoJson.lenientReader();
  private final ResourcePatternResolver resolver;

  public JsonResourceReader(ResourcePatternResolver resolver) {
    this.resolver = resolver;
  }

  /** Reads the file at {@code classpathLocation}, for example {@code processed/tripoints.json}. */
  public <T> T read(String classpathLocation, Class<T> type) {
    return parse(
        resolver.getResource("classpath:" + classpathLocation),
        stream -> objectMapper.readValue(stream, type));
  }

  public <T> T read(String classpathLocation, TypeReference<T> type) {
    return parse(
        resolver.getResource("classpath:" + classpathLocation),
        stream -> objectMapper.readValue(stream, type));
  }

  /** Reads every file matching a pattern such as {@code classpath*:/data/rivers/*.geo.json}. */
  public <T> List<T> readAll(String pattern, Class<T> type) {
    Resource[] resources;
    try {
      resources = resolver.getResources(pattern);
    } catch (IOException e) {
      throw new UncheckedIOException("Couldn't list the files matching " + pattern, e);
    }

    List<T> results = new ArrayList<>(resources.length);
    for (Resource resource : resources) {
      results.add(parse(resource, stream -> objectMapper.readValue(stream, type)));
    }
    return results;
  }

  private static <T> T parse(Resource resource, Function<InputStream, T> reader) {
    try (InputStream stream = resource.getInputStream()) {
      return reader.apply(stream);
    } catch (IOException e) {
      throw new UncheckedIOException("Couldn't read " + resource.getDescription(), e);
    } catch (JacksonException e) {
      throw new UncheckedIOException(
          "Couldn't parse " + resource.getDescription(), new IOException(e));
    }
  }
}
