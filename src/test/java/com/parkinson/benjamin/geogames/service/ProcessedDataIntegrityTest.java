package com.parkinson.benjamin.geogames.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.parkinson.benjamin.geogames.model.Country;
import com.parkinson.benjamin.geogames.model.River;
import com.parkinson.benjamin.geogames.model.TripointDefinition;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

public class ProcessedDataIntegrityTest {

  // Tripoints with no border junction nearby, removed during the country file migration.
  private static final List<Set<String>> DROPPED =
      List.of(
          Set.of("Somalia", "Djibouti", "Ethiopia"),
          Set.of("Ethiopia", "South Sudan", "Kenya"),
          Set.of("Azerbaijan", "Iran", "Turkmenistan"),
          Set.of("Russia", "Kazakhstan", "Azerbaijan"),
          Set.of("Kazakhstan", "Azerbaijan", "Turkmenistan"),
          Set.of("Pakistan", "India", "China"));

  private final JsonResourceReader reader =
      new JsonResourceReader(new PathMatchingResourcePatternResolver());
  private final TripointLoaderService tripointLoaderService = new TripointLoaderService(reader);
  private final CountryLoaderService countryLoaderService = new CountryLoaderService(reader);

  @Test
  public void tripointsAreWellFormed() throws IOException {
    List<TripointDefinition> definitions = tripointLoaderService.loadTripointDefinitions();

    assertEquals(164, definitions.size());
    for (TripointDefinition definition : definitions) {
      Set<String> names = new HashSet<>(definition.countryNames());
      assertEquals(3, names.size(), "expected 3 distinct countries in " + definition);
      assertFalse(DROPPED.contains(names), "dropped tripoint is back: " + names);
    }
  }

  @Test
  public void everyTripointCountryLoads() throws IOException {
    for (TripointDefinition definition : tripointLoaderService.loadTripointDefinitions()) {
      for (String name : definition.countryNames()) {
        Country country = countryLoaderService.loadCountryByName(name);
        assertEquals(name, country.name());
        assertNotNull(country.geoData().geometry(), "no geometry for " + name);
      }
    }
  }

  @Test
  public void riversHaveEndsAndCountriesThatLoad() {
    List<River> rivers = new RiverLoaderService(reader).loadRivers();

    assertEquals(150, rivers.size());
    for (River river : rivers) {
      assertNotNull(river.source(), "no source for " + river.name());
      assertNotNull(river.mouth(), "no mouth for " + river.name());
      assertNotEquals(river.source(), river.mouth(), "source is mouth for " + river.name());
      assertNotNull(river.countries(), "no countries for " + river.name());
      for (String name : river.countries()) {
        assertEquals(name, countryLoaderService.loadCountryByName(name).name());
      }
    }
  }

  @Test
  public void enoughRiversCrossTwoCountriesForTheCountriesGame() {
    long eligible =
        new RiverLoaderService(reader)
            .loadRivers().stream().filter(river -> river.countries().size() >= 2).count();

    assertTrue(eligible >= 20, "only " + eligible + " rivers cross two or more countries");
  }
}
