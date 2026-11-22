package com.parkinson.benjamin.geogames.loaders.rivers;

import com.parkinson.benjamin.geogames.loaders.features.NaturalEarthReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Corrects the Wikidata IDs of Natural Earth's river features in place, before any program reads
 * them: a feature whose name is in its override's {@code unlinkNames} is another river and loses
 * its ID, and a lake centerline with no ID takes the ID of the river that has the same {@code
 * rivernum} and {@code name}, so a river stays in one piece through its lakes.
 */
final class RiverQids {

  private static final String LAKE_CENTERLINE = "Lake Centerline";

  private RiverQids() {}

  static void correct(JsonNode featureCollection, Map<String, RiverOverride> overrides) {
    unlink(featureCollection, overrides);
    adoptLakeCenterlines(featureCollection);
  }

  private static void unlink(JsonNode featureCollection, Map<String, RiverOverride> overrides) {
    for (JsonNode feature : featureCollection.path("features")) {
      JsonNode props = feature.path("properties");
      RiverOverride override = overrides.get(NaturalEarthReader.wikidataId(props));
      if (override == null || override.unlinkNames() == null) {
        continue;
      }
      List<String> names = override.unlinkNames();
      if (names.contains(props.path("name").asString("")) && props instanceof ObjectNode node) {
        node.put("wikidataid", "");
      }
    }
  }

  private static void adoptLakeCenterlines(JsonNode featureCollection) {
    Map<String, Set<String>> qidsByKey = new HashMap<>();
    for (JsonNode feature : featureCollection.path("features")) {
      JsonNode props = feature.path("properties");
      String qid = NaturalEarthReader.wikidataId(props);
      if (!qid.isEmpty() && !isLakeCenterline(props)) {
        qidsByKey.computeIfAbsent(key(props), k -> new HashSet<>()).add(qid);
      }
    }
    for (JsonNode feature : featureCollection.path("features")) {
      JsonNode props = feature.path("properties");
      if (!isLakeCenterline(props)
          || !NaturalEarthReader.wikidataId(props).isEmpty()
          || !(props instanceof ObjectNode node)) {
        continue;
      }
      Set<String> qids = qidsByKey.get(key(props));
      if (qids != null && qids.size() == 1) {
        node.put("wikidataid", qids.iterator().next());
      }
    }
  }

  private static boolean isLakeCenterline(JsonNode props) {
    return LAKE_CENTERLINE.equals(props.path("featurecla").asString(""));
  }

  private static String key(JsonNode props) {
    return props.path("rivernum").asString("") + "|" + props.path("name").asString("");
  }
}
