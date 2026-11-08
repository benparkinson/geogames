package com.parkinson.benjamin.geogames.loaders.features;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Picks the features to write: the most famous of each continent, by Wikipedia sitelinks, up to that
 * continent's quota. Overrides only correct a feature's data; they don't force it in.
 */
public final class FeatureSelector {

  /** What the selector needs to know about a candidate. */
  public interface RankedFeature {
    String qid();

    String continent();

    int sitelinks();
  }

  /**
   * @param selected the features to write
   * @param unresolved famous enough to have been selected, but skipped for lack of a continent
   * @param beyondLimit how many resolved features were left out by the quotas
   */
  public record Selection<T extends RankedFeature>(
      List<T> selected, List<T> unresolved, int beyondLimit) {}

  private FeatureSelector() {}

  public static <T extends RankedFeature> Selection<T> select(
      List<T> candidates, Map<String, Integer> quotas) {
    List<T> ranked =
        candidates.stream()
            .sorted(Comparator.comparingInt(T::sitelinks).reversed().thenComparing(T::qid))
            .toList();
    Map<String, Integer> taken = new HashMap<>();
    Map<String, Integer> lowestSitelinks = new HashMap<>();
    List<T> selected = new ArrayList<>();
    int beyondLimit = 0;
    for (T candidate : ranked) {
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

    // Only report unresolved features famous enough to have made the cut on any continent.
    int cutoff = lowestSitelinks.values().stream().mapToInt(Integer::intValue).max().orElse(0);
    List<T> unresolved =
        ranked.stream().filter(c -> c.continent() == null && c.sitelinks() >= cutoff).toList();
    return new Selection<>(selected, unresolved, beyondLimit);
  }
}
