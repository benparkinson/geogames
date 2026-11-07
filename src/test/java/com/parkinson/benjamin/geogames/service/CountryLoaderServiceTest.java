package com.parkinson.benjamin.geogames.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.parkinson.benjamin.geogames.model.CountryName;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

class CountryLoaderServiceTest {

  private final CountryLoaderService service =
      new CountryLoaderService(new JsonResourceReader(new PathMatchingResourcePatternResolver()));

  @Test
  void loadsEveryCountryNameSorted() throws IOException {
    List<CountryName> names = service.loadCountryNames();

    assertEquals(255, names.size());
    assertEquals(names.stream().sorted(Comparator.comparing(CountryName::name)).toList(), names);
    assertTrue(names.stream().anyMatch(c -> "France".equals(c.name())));
    names.forEach(c -> assertNotNull(c.additionalNames(), c.name()));
  }
}
