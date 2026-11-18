package com.parkinson.benjamin.geogames.loaders.countries;

import com.parkinson.benjamin.geogames.helper.GeoJson;
import com.parkinson.benjamin.geogames.helper.PlaceNames;
import com.parkinson.benjamin.geogames.model.Country;
import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import com.parkinson.benjamin.geogames.model.geojson.GeoProperties;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import tools.jackson.databind.ObjectMapper;

/**
 * Reads the raw Natural Earth export plus the hand-written name files into {@link Country} objects.
 * Features named in the exclusion file (non-country features such as bases and reefs) are skipped.
 * The official names file replaces a feature's outdated or badly spelled name, and the old name
 * stays accepted as an alias. The alias file is keyed by the name a country ends up with.
 */
public class RawCountryReader {

  private final ObjectMapper objectMapper = GeoJson.lenientReader();

  /**
   * @throws IllegalArgumentException if an excluded or renamed name matches no feature in the
   *     source, or the aliases are for a country that doesn't exist
   */
  @SuppressWarnings("unchecked")
  public List<Country> readCountries(
      Path countriesGeoJson,
      Path officialNamesJson,
      Path additionalNamesJson,
      Path excludedNamesJson)
      throws IOException {
    GeoData[] countryGeoData = objectMapper.readValue(countriesGeoJson.toFile(), GeoData[].class);
    Map<String, String> officialNames =
        objectMapper.readValue(officialNamesJson.toFile(), Map.class);
    Map<String, List<String>> additionalNamesByCountryName =
        objectMapper.readValue(additionalNamesJson.toFile(), Map.class);
    Set<String> excludedNames =
        new LinkedHashSet<>(
            Arrays.asList(objectMapper.readValue(excludedNamesJson.toFile(), String[].class)));

    Set<String> sourceNames =
        Arrays.stream(countryGeoData)
            .map(geoData -> geoData.properties().name())
            .collect(Collectors.toSet());
    requireAllIn(sourceNames, excludedNames, "Excluded names", countriesGeoJson);
    requireAllIn(sourceNames, officialNames.keySet(), "Renamed names", countriesGeoJson);

    List<Country> countries =
        Arrays.stream(countryGeoData)
            .filter(geoData -> !excludedNames.contains(geoData.properties().name()))
            .map(geoData -> toCountry(geoData, officialNames, additionalNamesByCountryName))
            .toList();

    requireAllIn(
        countries.stream().map(Country::name).collect(Collectors.toSet()),
        additionalNamesByCountryName.keySet(),
        "Alias keys",
        countriesGeoJson);
    return countries;
  }

  private static Country toCountry(
      GeoData geoData,
      Map<String, String> officialNames,
      Map<String, List<String>> additionalNamesByCountryName) {
    String sourceName = geoData.properties().name();
    String name = officialNames.getOrDefault(sourceName, sourceName);
    List<String> aliases = new ArrayList<>();
    if (!PlaceNames.normalise(sourceName).equals(PlaceNames.normalise(name))) {
      aliases.add(sourceName);
    }
    aliases.addAll(additionalNamesByCountryName.getOrDefault(name, List.of()));
    GeoData renamed = new GeoData(geoData.type(), new GeoProperties(name), geoData.geometry());
    return new Country(name, aliases, renamed);
  }

  private static void requireAllIn(
      Set<String> known, Set<String> names, String description, Path source) {
    Set<String> unmatched = new LinkedHashSet<>(names);
    unmatched.removeAll(known);
    if (!unmatched.isEmpty()) {
      throw new IllegalArgumentException(
          description + " match no feature in " + source + ": " + unmatched);
    }
  }
}
