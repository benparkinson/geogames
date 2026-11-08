package com.parkinson.benjamin.geogames.loaders.lakes;

import com.parkinson.benjamin.geogames.loaders.features.AliasBuilder;
import com.parkinson.benjamin.geogames.loaders.features.ContinentResolver;
import com.parkinson.benjamin.geogames.loaders.features.NaturalEarthFeature;
import com.parkinson.benjamin.geogames.loaders.features.WikidataEntry;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Combines Natural Earth, Wikidata and the overrides into candidate lakes. Reads no files. */
public class LakeAssembler {

  static final double MIN_AREA_KM2 = 100;

  /** The candidates, plus how many lakes were dropped for each reason. */
  public record Assembly(List<LakeCandidate> candidates, Map<String, Integer> dropped) {}

  public Assembly assemble(
      Map<String, NaturalEarthFeature> groups,
      Map<String, WikidataEntry> wikidata,
      Map<String, LakeOverride> overrides) {
    Map<String, Integer> dropped = new LinkedHashMap<>();

    Set<String> qids = new LinkedHashSet<>();
    for (String qid : groups.keySet()) {
      LakeOverride override = overrides.get(qid);
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

    List<LakeCandidate> candidates = new ArrayList<>();
    for (String qid : qids) {
      LakeOverride override = overrides.get(qid);
      WikidataEntry entry = wikidata.get(qid);
      NaturalEarthFeature group = groups.get(qid);
      boolean fromOverride = override != null;

      List<List<List<double[]>>> polygons =
          LakeGeometry.simplify(group == null ? List.of() : group.polygons());
      if (polygons.isEmpty()) {
        drop(dropped, "no geometry");
        continue;
      }
      double area = LakeGeometry.areaKm2(polygons);
      if (!fromOverride && area < MIN_AREA_KM2) {
        drop(dropped, "smaller than " + (int) MIN_AREA_KM2 + " km²");
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
        drop(dropped, "override lake with no continent");
        continue;
      }

      List<String> aliases =
          AliasBuilder.build(
              name,
              entry == null ? List.of() : entry.aliases(),
              group == null ? null : group.name(),
              group == null ? null : group.nameEn(),
              override,
              LakeAssembler::lakeVariants);
      candidates.add(
          new LakeCandidate(
              qid,
              name,
              aliases,
              continent,
              entry == null ? 0 : entry.sitelinks(),
              fromOverride,
              polygons,
              area));
    }
    return new Assembly(candidates, dropped);
  }

  /**
   * "Lake X" is also accepted as "X", "X Lake" as "X", and any other name as "Lake X". Names
   * ending in "Sea" get none, and neither does a variant that starts with a lower-case letter
   * ("Lake of the Woods" is not "of the Woods").
   */
  public static List<String> lakeVariants(String name) {
    String variant;
    if (name.endsWith(" Sea")) {
      return List.of();
    } else if (name.startsWith("Lake ")) {
      variant = name.substring("Lake ".length());
    } else if (name.endsWith(" Lake")) {
      variant = name.substring(0, name.length() - " Lake".length());
    } else {
      variant = "Lake " + name;
    }
    if (variant.isBlank() || Character.isLowerCase(variant.charAt(0))) {
      return List.of();
    }
    return List.of(variant);
  }

  private static String name(
      LakeOverride override, WikidataEntry entry, NaturalEarthFeature group) {
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
