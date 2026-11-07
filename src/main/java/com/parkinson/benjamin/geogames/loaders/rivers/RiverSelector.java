package com.parkinson.benjamin.geogames.loaders.rivers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Picks the rivers to write: the most famous of each continent, by Wikipedia sitelinks, up to that
 * continent's quota. Overrides only correct a river's data; they don't force it in.
 */
public final class RiverSelector {

  /** How many rivers each continent gets, so the game isn't all Europe and Asia. */
  public static final Map<String, Integer> CONTINENT_QUOTAS =
      Map.of(
          "Europe", 13,
          "Asia", 11,
          "Africa", 9,
          "North America", 6,
          "South America", 8,
          "Australia", 3);

  /**
   * @param selected the rivers to write
   * @param unresolved famous enough to have been selected, but skipped for lack of a continent
   * @param beyondLimit how many resolved rivers were left out by the quotas
   */
  public record Selection(
      List<RiverCandidate> selected, List<RiverCandidate> unresolved, int beyondLimit) {}

  private RiverSelector() {}

  public static Selection select(List<RiverCandidate> candidates, Map<String, Integer> quotas) {
    List<RiverCandidate> ranked =
        candidates.stream()
            .sorted(
                Comparator.comparingInt(RiverCandidate::sitelinks)
                    .reversed()
                    .thenComparing(RiverCandidate::qid))
            .toList();
    Map<String, Integer> taken = new HashMap<>();
    Map<String, Integer> lowestSitelinks = new HashMap<>();
    List<RiverCandidate> selected = new ArrayList<>();
    int beyondLimit = 0;
    for (RiverCandidate candidate : ranked) {
      if (candidate.continent() == null) {
        continue;
      }
      int count = taken.getOrDefault(candidate.continent(), 0);
      if (count < quotas.getOrDefault(candidate.continent(), 0)) {
        selected.add(candidate);
        taken.put(candidate.continent(), count + 1);
        lowestSitelinks.put(candidate.continent(), candidate.sitelinks());
      } else {
        beyondLimit++;
      }
    }

    // Only report unresolved rivers famous enough to have made the cut on any continent.
    int cutoff = lowestSitelinks.values().stream().mapToInt(Integer::intValue).max().orElse(0);
    List<RiverCandidate> unresolved =
        ranked.stream().filter(c -> c.continent() == null && c.sitelinks() >= cutoff).toList();
    return new Selection(selected, unresolved, beyondLimit);
  }
}
