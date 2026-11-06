package com.parkinson.benjamin.geogames.service;

import java.io.IOException;
import java.io.InputStream;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public abstract class FileLoaderService {

  private final ObjectMapper objectMapper =
      JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();

  protected <T> T loadFile(String resourceFileName, Class<T> clazz) throws IOException {
    try (InputStream inputStream = new ClassPathResource(resourceFileName).getInputStream()) {
      return readFile(inputStream, clazz);
    }
  }

  protected <T> T readFile(InputStream inputStream, Class<T> clazz) throws IOException {
    return objectMapper.readValue(inputStream, clazz);
  }
}
