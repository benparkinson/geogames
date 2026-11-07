package com.parkinson.benjamin.geogames.loaders.rivers;

import java.util.List;

/** What {@code wikidata.json} holds about one river. Lists are empty, never null, when unknown. */
public record WikidataEntry(
    String label,
    int sitelinks,
    List<String> aliases,
    List<String> continents,
    List<String> countryContinents) {

  public WikidataEntry {
    aliases = aliases == null ? List.of() : aliases;
    continents = continents == null ? List.of() : continents;
    countryContinents = countryContinents == null ? List.of() : countryContinents;
  }
}
