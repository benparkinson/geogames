package com.parkinson.benjamin.geogames.loaders.rivers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/** Combines Natural Earth, Wikidata and the overrides into candidate rivers. Reads no files. */
public class RiverAssembler {

  static final double MIN_RIVER_KM = 200;

  /** The candidates, plus how many rivers were dropped for each reason. */
  public record Assembly(List<RiverCandidate> candidates, Map<String, Integer> dropped) {}

  private final Function<String, List<List<double[]>>> osmLines;

  /** @param osmLines gives the lines of an OSM file by file name */
  public RiverAssembler(Function<String, List<List<double[]>>> osmLines) {
    this.osmLines = osmLines;
  }

  public Assembly assemble(
      Map<String, NaturalEarthRiver> groups,
      Map<String, WikidataEntry> wikidata,
      Map<String, RiverOverride> overrides) {
    Map<String, Integer> dropped = new LinkedHashMap<>();

    Set<String> qids = new LinkedHashSet<>();
    for (String qid : groups.keySet()) {
      RiverOverride override = overrides.get(qid);
      if (override != null && override.isExcluded()) {
        drop(dropped, "excluded by override");
      } else if (wikidata.containsKey(qid)) {
        qids.add(qid);
      } else if (override == null) {
        drop(dropped, "no Wikidata entry");
      } else {
        qids.add(qid);
      }
    }
    overrides.forEach(
        (qid, override) -> {
          if (!override.isExcluded()) {
            qids.add(qid);
          }
        });

    List<RiverCandidate> candidates = new ArrayList<>();
    for (String qid : qids) {
      RiverOverride override = overrides.get(qid);
      WikidataEntry entry = wikidata.get(qid);
      NaturalEarthRiver group = groups.get(qid);
      boolean fromOverride = override != null;

      List<List<double[]>> raw =
          fromOverride && override.osmFile() != null
              ? osmLines.apply(override.osmFile())
              : group == null ? List.of() : group.lines();
      List<List<double[]>> lines = RiverGeometry.simplify(raw);
      if (lines.isEmpty()) {
        drop(dropped, "no geometry");
        continue;
      }
      double km = RiverGeometry.lengthKm(lines);
      if (!fromOverride && km < MIN_RIVER_KM) {
        drop(dropped, "shorter than " + (int) MIN_RIVER_KM + " km");
        continue;
      }

      String name = name(override, entry, group);
      if (name == null) {
        drop(dropped, "no name");
        continue;
      }

      String continent =
          ContinentResolver.resolve(
                  override == null ? null : override.continent(),
                  entry == null ? List.of() : entry.continents(),
                  entry == null ? List.of() : entry.countryContinents())
              .orElse(null);
      if (continent == null && fromOverride) {
        drop(dropped, "override river with no continent");
        continue;
      }

      List<String> aliases =
          AliasBuilder.build(
              name,
              entry == null ? List.of() : entry.aliases(),
              group == null ? null : group.name(),
              group == null ? null : group.nameEn(),
              override);
      candidates.add(
          new RiverCandidate(
              qid,
              name,
              aliases,
              continent,
              entry == null ? 0 : entry.sitelinks(),
              fromOverride,
              lines,
              km));
    }
    return new Assembly(candidates, dropped);
  }

  private static String name(RiverOverride override, WikidataEntry entry, NaturalEarthRiver group) {
    String[] options = {
      override == null ? null : override.name(),
      entry == null ? null : entry.label(),
      group == null ? null : group.nameEn(),
      group == null ? null : group.name()
    };
    for (String option : options) {
      if (option != null && !option.isBlank()) {
        return option;
      }
    }
    return null;
  }

  private static void drop(Map<String, Integer> dropped, String reason) {
    dropped.merge(reason, 1, Integer::sum);
  }
}
