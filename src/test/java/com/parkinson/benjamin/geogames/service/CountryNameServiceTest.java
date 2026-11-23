package com.parkinson.benjamin.geogames.service;

import static com.parkinson.benjamin.geogames.TestFixtures.EXPECTED_COUNTRY_COUNT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.parkinson.benjamin.geogames.model.LocatedPlaceName;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

class CountryNameServiceTest {

  private final CountryNameService service =
      new CountryNameService(
          new CountryShapeService(
              new JsonResourceReader(new PathMatchingResourcePatternResolver())));

  @Test
  void loadsEveryCountryNameSorted() throws IOException {
    List<LocatedPlaceName> names = service.loadCountryNames();

    assertEquals(EXPECTED_COUNTRY_COUNT, names.size());
    assertTrue(names.stream().noneMatch(c -> "Siachen Glacier".equals(c.name())));
    assertTrue(names.stream().noneMatch(c -> "US Naval Base Guantanamo Bay".equals(c.name())));
    assertEquals(
        names.stream().sorted(Comparator.comparing(LocatedPlaceName::name)).toList(), names);
    assertTrue(names.stream().noneMatch(c -> "Antarctica".equals(c.name())));
    assertTrue(names.stream().noneMatch(c -> "Northern Cyprus".equals(c.name())));
    assertTrue(names.stream().anyMatch(c -> "Palestine".equals(c.name())));
    assertTrue(names.stream().anyMatch(c -> "France".equals(c.name())));
    names.forEach(c -> assertNotNull(c.additionalNames(), c.name()));
  }

  @Test
  void positionsAreOnTheMainlandOfTheCountry() {
    List<LocatedPlaceName> names = service.loadCountryNames();
    LocatedPlaceName france =
        names.stream().filter(c -> "France".equals(c.name())).findFirst().get();
    LocatedPlaceName russia =
        names.stream().filter(c -> "Russia".equals(c.name())).findFirst().get();

    // Mainland France, not pulled towards French Guiana.
    assertTrue(france.lat() > 45 && france.lat() < 48, "lat " + france.lat());
    assertTrue(france.lon() > 1 && france.lon() < 4, "lon " + france.lon());
    assertTrue(russia.lon() > 40 && russia.lon() < 110, "lon " + russia.lon());
  }
}
