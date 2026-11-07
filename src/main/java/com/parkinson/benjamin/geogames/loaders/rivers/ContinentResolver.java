package com.parkinson.benjamin.geogames.loaders.rivers;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Maps Wikidata continent labels onto the six continents the game knows. */
public final class ContinentResolver {

  private static final Set<String> AUSTRALIA =
      Set.of("Oceania", "Insular Oceania", "Australia", "Australian continent");
  private static final Set<String> PASS_THROUGH =
      Set.of("Africa", "Asia", "Europe", "North America", "South America");

  private ContinentResolver() {}

  /** The game's continent for a label, or empty for anything else (Antarctica, Eurasia, ...). */
  public static Optional<String> map(String label) {
    if (AUSTRALIA.contains(label)) {
      return Optional.of("Australia");
    }
    return PASS_THROUGH.contains(label) ? Optional.of(label) : Optional.empty();
  }

  /** The override, else the river's own continent, else the countries'. Empty if unresolved. */
  public static Optional<String> resolve(
      String overrideContinent, List<String> continents, List<String> countryContinents) {
    if (overrideContinent != null && !overrideContinent.isBlank()) {
      return Optional.of(overrideContinent);
    }
    Optional<String> direct = single(continents);
    return direct.isPresent() ? direct : single(countryContinents);
  }

  private static Optional<String> single(List<String> labels) {
    Set<String> mapped =
        labels.stream()
            .map(ContinentResolver::map)
            .flatMap(Optional::stream)
            .collect(Collectors.toSet());
    return mapped.size() == 1 ? Optional.of(mapped.iterator().next()) : Optional.empty();
  }
}
