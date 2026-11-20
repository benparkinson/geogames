package com.parkinson.benjamin.geogames.loaders.countries;

import com.parkinson.benjamin.geogames.helper.GeoJson;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import tools.jackson.core.type.TypeReference;

/** Reads {@code geodata/countries/continents.json} and checks it against the countries built. */
final class CountryContinents {

  static final Set<String> CONTINENTS =
      Set.of("Africa", "Asia", "Europe", "North America", "South America", "Oceania");

  private CountryContinents() {}

  /**
   * The continents of each country, keyed by the game's country name.
   *
   * @param shapeNames the countries that need an entry
   * @throws IllegalArgumentException if a country has no entry, an entry matches no country, or a
   *     value is empty, has more than two continents or names an unknown continent
   */
  static Map<String, List<String>> read(Path file, Set<String> shapeNames) throws IOException {
    Map<String, List<String>> continents =
        GeoJson.lenientReader().readValue(file.toFile(), new TypeReference<>() {});
    List<String> problems = new ArrayList<>();
    for (String name : new TreeSet<>(shapeNames)) {
      if (!continents.containsKey(name)) {
        problems.add("no continents for " + name);
      }
    }
    for (Map.Entry<String, List<String>> entry : new java.util.TreeMap<>(continents).entrySet()) {
      String name = entry.getKey();
      List<String> value = entry.getValue();
      if (!shapeNames.contains(name)) {
        problems.add("continents.json names " + name + ", which matches no country shape");
      } else if (value == null || value.isEmpty() || value.size() > 2) {
        problems.add(name + " needs one or two continents, not " + value);
      } else if (!CONTINENTS.containsAll(value)) {
        problems.add(name + " has an unknown continent in " + value);
      } else if (value.size() == 2 && value.get(0).equals(value.get(1))) {
        problems.add(name + " lists " + value.get(0) + " twice");
      }
    }
    if (!problems.isEmpty()) {
      throw new IllegalArgumentException("Bad " + file + ": " + String.join("; ", problems));
    }
    return continents;
  }
}
