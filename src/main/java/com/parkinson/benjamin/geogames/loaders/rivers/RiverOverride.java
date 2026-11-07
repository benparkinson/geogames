package com.parkinson.benjamin.geogames.loaders.rivers;

import com.parkinson.benjamin.geogames.model.Coordinate;
import java.util.List;

/**
 * One hand-written entry of {@code overrides.json}, keyed by Wikidata QID. All fields optional.
 *
 * <p>{@code reverseFlow} swaps the computed source and mouth, for rivers digitised upstream.
 * {@code source} and {@code mouth} replace the computed ends outright.
 */
public record RiverOverride(
    Boolean exclude,
    String name,
    String continent,
    List<String> aliases,
    List<String> addAliases,
    List<String> removeAliases,
    String osmFile,
    Boolean reverseFlow,
    Coordinate source,
    Coordinate mouth) {

  public boolean isExcluded() {
    return Boolean.TRUE.equals(exclude);
  }
}
