package com.parkinson.benjamin.geogames.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.core.io.ClassPathResource;

public abstract class FileLoaderService {

  private final ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  protected <T> T loadFile(String resourceFileName, Class<T> clazz) throws IOException {
    InputStream coordinates = new ClassPathResource(resourceFileName).getInputStream();

    return readFile(coordinates, clazz);
  }

  protected <T> T readFile(InputStream inputStream, Class<T> clazz) throws IOException {
    return objectMapper.readValue(inputStream, clazz);
  }
}
