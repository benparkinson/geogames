package com.parkinson.benjamin.geogames.service;

import static com.parkinson.benjamin.geogames.TestFixtures.EXPECTED_COUNTRY_COUNT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.parkinson.benjamin.geogames.model.PlaceName;
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
    List<PlaceName> names = service.loadCountryNames();

    assertEquals(EXPECTED_COUNTRY_COUNT, names.size());
    assertTrue(names.stream().noneMatch(c -> "Siachen Glacier".equals(c.name())));
    assertTrue(names.stream().noneMatch(c -> "US Naval Base Guantanamo Bay".equals(c.name())));
    assertEquals(names.stream().sorted(Comparator.comparing(PlaceName::name)).toList(), names);
    assertTrue(names.stream().anyMatch(c -> "France".equals(c.name())));
    names.forEach(c -> assertNotNull(c.additionalNames(), c.name()));
  }
}
