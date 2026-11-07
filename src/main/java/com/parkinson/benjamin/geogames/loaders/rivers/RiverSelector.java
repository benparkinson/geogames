package com.parkinson.benjamin.geogames.loaders.rivers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Picks the rivers to write: the most famous, plus every override river. */
public final class RiverSelector {

  /**
   * @param selected the rivers to write
   * @param unresolved famous enough to have been selected, but skipped for lack of a continent
   * @param beyondLimit how many resolved rivers were left out by the limit
   */
  public record Selection(
      List<RiverCandidate> selected, List<RiverCandidate> unresolved, int beyondLimit) {}

  private RiverSelector() {}

  public static Selection select(List<RiverCandidate> candidates, int maxRivers) {
    List<RiverCandidate> overrides =
        candidates.stream().filter(RiverCandidate::fromOverride).toList();
    List<RiverCandidate> others =
        candidates.stream()
            .filter(c -> !c.fromOverride())
            .sorted(
                Comparator.comparingInt(RiverCandidate::sitelinks)
                    .reversed()
                    .thenComparing(RiverCandidate::qid))
            .toList();

    int quota = Math.max(0, maxRivers - overrides.size());
    List<RiverCandidate> selected = new ArrayList<>(overrides);
    List<RiverCandidate> unresolved = new ArrayList<>();
    int taken = 0;
    int beyondLimit = 0;
    for (RiverCandidate candidate : others) {
      if (taken >= quota) {
        if (candidate.continent() != null) {
          beyondLimit++;
        }
      } else if (candidate.continent() == null) {
        unresolved.add(candidate);
      } else {
        selected.add(candidate);
        taken++;
      }
    }
    return new Selection(selected, unresolved, beyondLimit);
  }
}
