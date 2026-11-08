package com.parkinson.benjamin.geogames.loaders.features;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

/** Works out the accepted alternative names of a named feature. */
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

  /** The hand-written correction of a feature; the fields the alias rules read. */
  public interface NamedOverride {
    Boolean exclude();

    String name();

    String continent();

    List<String> aliases();

    List<String> addAliases();

    List<String> removeAliases();

    default boolean isExcluded() {
      return Boolean.TRUE.equals(exclude());
    }
  }

  /**
   * @param wikidataAliases English aliases from Wikidata
   * @param neName Natural Earth {@code name}, may be null
   * @param neNameEn Natural Earth {@code name_en}, may be null
   * @param override the override entry, may be null
   * @param variants the name variants that are also accepted ("Lake X" for "X"); candidates are
   *     added in the order returned
   */
  public static List<String> build(
      String name,
      List<String> wikidataAliases,
      String neName,
      String neNameEn,
      NamedOverride override,
      Function<String, List<String>> variants) {
    if (override != null && override.aliases() != null) {
      return List.copyOf(override.aliases());
    }

    List<String> candidates = new ArrayList<>(wikidataAliases);
    addIfPresent(candidates, neName);
    addIfPresent(candidates, neNameEn);
    candidates.addAll(variants.apply(name));

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

  private static void addIfPresent(List<String> list, String value) {
    if (value != null && !value.isBlank()) {
      list.add(value);
    }
  }
}
