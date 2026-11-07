package com.parkinson.benjamin.geogames.loaders.rivers;

import java.util.List;

/** One hand-written entry of {@code overrides.json}, keyed by Wikidata QID. All fields optional. */
public record RiverOverride(
    Boolean exclude,
    String name,
    String continent,
    List<String> aliases,
    List<String> addAliases,
    List<String> removeAliases,
    String osmFile) {

  public boolean isExcluded() {
    return Boolean.TRUE.equals(exclude);
  }
}
