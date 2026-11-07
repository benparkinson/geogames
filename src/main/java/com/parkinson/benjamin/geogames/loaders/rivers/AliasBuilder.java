package com.parkinson.benjamin.geogames.loaders.rivers;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Works out the accepted alternative names of a river. */
public final class AliasBuilder {

  private static final int MAX_ALIAS_LENGTH = 40;

  private AliasBuilder() {}

  /** A Java copy of the frontend's {@code normaliseString}. */
  public static String normalise(String s) {
    return Normalizer.normalize(s, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9]", "");
  }

  /**
   * @param wikidataAliases English aliases from Wikidata
   * @param neName Natural Earth {@code name}, may be null
   * @param neNameEn Natural Earth {@code name_en}, may be null
   * @param override the override entry, may be null
   */
  public static List<String> build(
      String name,
      List<String> wikidataAliases,
      String neName,
      String neNameEn,
      RiverOverride override) {
    if (override != null && override.aliases() != null) {
      return List.copyOf(override.aliases());
    }

    List<String> candidates = new ArrayList<>(wikidataAliases);
    addIfPresent(candidates, neName);
    addIfPresent(candidates, neNameEn);
    candidates.add(riverVariant(name));

    if (override != null && override.addAliases() != null) {
      candidates.addAll(override.addAliases());
    }
    if (override != null && override.removeAliases() != null) {
      Set<String> removed = new HashSet<>();
      override.removeAliases().forEach(a -> removed.add(normalise(a)));
      candidates.removeIf(a -> removed.contains(normalise(a)));
    }

    Set<String> seen = new HashSet<>();
    seen.add(normalise(name));
    List<String> result = new ArrayList<>();
    for (String alias : candidates) {
      String normalised = normalise(alias);
      if (normalised.isEmpty()
          || alias.length() > MAX_ALIAS_LENGTH
          || alias.chars().anyMatch(Character::isDigit)
          || !seen.add(normalised)) {
        continue;
      }
      result.add(alias);
    }
    return result;
  }

  private static String riverVariant(String name) {
    if (name.endsWith(" River")) {
      return name.substring(0, name.length() - " River".length());
    }
    return name.contains("River") ? name : name + " River";
  }

  private static void addIfPresent(List<String> list, String value) {
    if (value != null && !value.isBlank()) {
      list.add(value);
    }
  }
}
