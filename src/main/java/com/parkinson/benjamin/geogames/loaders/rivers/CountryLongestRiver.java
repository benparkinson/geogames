package com.parkinson.benjamin.geogames.loaders.rivers;

import com.parkinson.benjamin.geogames.loaders.features.Geo;
import com.parkinson.benjamin.geogames.loaders.rivers.RiverClipper.Run;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Picks the river to name in Waterways' first hint: the longest named river inside the
 * country. The name helps even when the player doesn't know the river, because its language narrows
 * the answer down.
 */
final class CountryLongestRiver {

  /**
   * A river with less than this in the country is never named: a short stretch of a river that is
   * mostly somewhere else shouldn't be called the country's longest river.
   */
  static final double MIN_KM = 50;

  private CountryLongestRiver() {}

  /**
   * The name to show, or empty when no river qualifies.
   *
   * @param countryNames the country's name and aliases: a river whose name or alias contains any of
   *     them as a whole word is skipped, and so is one whose whole name is a part of theirs, because
   *     naming it would give the country away (the Niger
   *     for Niger, the Congo for either Congo)
   * @param runs the country's stretches of river, before simplifying or the point limit
   * @param candidates the named rivers, by Wikidata ID. Runs with no ID, or an ID that isn't here,
   *     are ignored
   */
  static Optional<String> of(
      List<String> countryNames, List<Run> runs, Map<String, RiverCandidate> candidates) {
    Map<String, Double> kmByQid = new HashMap<>();
    for (Run run : runs) {
      if (run.qid() != null && candidates.containsKey(run.qid())) {
        kmByQid.merge(run.qid(), Geo.lineKm(run.points()), Double::sum);
      }
    }
    List<List<String>> countryWords =
        countryNames.stream().map(CountryLongestRiver::words).toList();
    return kmByQid.entrySet().stream()
        .filter(e -> e.getValue() >= MIN_KM)
        .map(e -> Map.entry(candidates.get(e.getKey()), e.getValue()))
        .filter(e -> !givesAway(e.getKey(), countryWords))
        .max(
            Comparator.<Map.Entry<RiverCandidate, Double>>comparingDouble(Map.Entry::getValue)
                .thenComparing(e -> e.getKey().name(), Comparator.reverseOrder()))
        .map(e -> e.getKey().name());
  }

  private static boolean givesAway(RiverCandidate river, List<List<String>> countryWords) {
    List<String> names = new ArrayList<>(river.aliases());
    names.add(river.name());
    for (String name : names) {
      List<String> riverWords = words(name);
      for (List<String> country : countryWords) {
        if (containsRun(riverWords, country) || containsRun(country, riverWords)) {
          return true;
        }
      }
    }
    return false;
  }

  /** Whether {@code words} contains {@code part} as consecutive whole words. */
  private static boolean containsRun(List<String> words, List<String> part) {
    if (part.isEmpty()) {
      return false;
    }
    for (int i = 0; i + part.size() <= words.size(); i++) {
      if (words.subList(i, i + part.size()).equals(part)) {
        return true;
      }
    }
    return false;
  }

  /** Lower case, without accents, split at anything that is not a letter or digit. */
  private static List<String> words(String name) {
    String plain =
        Normalizer.normalize(name, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT);
    List<String> words = new ArrayList<>();
    for (String word : plain.split("[^a-z0-9]+")) {
      if (!word.isEmpty()) {
        words.add(word);
      }
    }
    return words;
  }
}
