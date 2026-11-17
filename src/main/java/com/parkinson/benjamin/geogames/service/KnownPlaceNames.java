package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.helper.PlaceNames;
import com.parkinson.benjamin.geogames.model.PlaceKind;
import com.parkinson.benjamin.geogames.model.PlaceName;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Finds the list spelling of a guessed place name, so the server stores only known names and never
 * the text as typed. The maps are small and keyed by place name, not by round or game.
 */
@Service
public class KnownPlaceNames {

  private final CountryLoaderService countries;
  private final RiverLoaderService rivers;
  private final LakeLoaderService lakes;
  private final Map<PlaceKind, Map<String, String>> byKind = new EnumMap<>(PlaceKind.class);

  public KnownPlaceNames(
      CountryLoaderService countries, RiverLoaderService rivers, LakeLoaderService lakes) {
    this.countries = countries;
    this.rivers = rivers;
    this.lakes = lakes;
  }

  /** The name as the list spells it, or empty if the guess is not a name or alias of that kind. */
  public Optional<String> resolve(PlaceKind kind, String guess) {
    if (guess == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(lookup(kind).get(PlaceNames.normalise(guess)));
  }

  private synchronized Map<String, String> lookup(PlaceKind kind) {
    return byKind.computeIfAbsent(kind, k -> build(names(k)));
  }

  private List<PlaceName> names(PlaceKind kind) {
    return switch (kind) {
      case COUNTRIES ->
          countries.loadCountryNames().stream()
              .map(c -> new PlaceName(c.name(), c.additionalNames()))
              .toList();
      case RIVERS -> rivers.loadRiverNames();
      case LAKES -> lakes.loadLakeNames();
    };
  }

  /** Names go in before aliases, so a name wins over another place's alias. Each maps to its own spelling. */
  private static Map<String, String> build(List<PlaceName> names) {
    Map<String, String> map = new HashMap<>();
    for (PlaceName place : names) {
      map.putIfAbsent(PlaceNames.normalise(place.name()), place.name());
    }
    for (PlaceName place : names) {
      if (place.additionalNames() == null) {
        continue;
      }
      for (String alias : place.additionalNames()) {
        map.putIfAbsent(PlaceNames.normalise(alias), alias);
      }
    }
    return map;
  }
}
