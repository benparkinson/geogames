package com.parkinson.benjamin.geogames.model;

import java.util.List;

public record PlaceName(String name, List<String> additionalNames) {

  /** A name with its aliases, where a missing list (a file without aliases) means none. */
  public static PlaceName of(String name, List<String> additionalNames) {
    return new PlaceName(name, additionalNames == null ? List.of() : additionalNames);
  }
}
