package com.parkinson.benjamin.geogames.loaders.rivers;

import com.parkinson.benjamin.geogames.helper.GeoJson;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;

/** Reads the Natural Earth river file and groups its features by Wikidata ID. */
public class NaturalEarthReader {

  public Map<String, NaturalEarthRiver> read(Path file) throws IOException {
    try (InputStream in = Files.newInputStream(file)) {
      return group(GeoJson.lenientReader().readTree(in));
    }
  }

  /**
   * Groups line features by {@code wikidataid}. Features without one are skipped, and so are
   * canals. Lake centerlines are kept, because they keep a river continuous through a lake.
   */
  public Map<String, NaturalEarthRiver> group(JsonNode featureCollection) {
    Map<String, String> names = new LinkedHashMap<>();
    Map<String, String> namesEn = new LinkedHashMap<>();
    Map<String, List<List<double[]>>> lines = new LinkedHashMap<>();

    for (JsonNode feature : featureCollection.path("features")) {
      JsonNode props = feature.path("properties");
      String qid = text(props.path("wikidataid"));
      if (qid.isEmpty() || "Canal".equals(text(props.path("featurecla")))) {
        continue;
      }
      List<List<double[]>> parts = lines.computeIfAbsent(qid, k -> new ArrayList<>());
      JsonNode geometry = feature.path("geometry");
      String type = text(geometry.path("type"));
      if (type.equals("LineString")) {
        parts.add(line(geometry.path("coordinates")));
      } else if (type.equals("MultiLineString")) {
        for (JsonNode part : geometry.path("coordinates")) {
          parts.add(line(part));
        }
      }
      names.merge(qid, text(props.path("name")), (old, neu) -> old.isEmpty() ? neu : old);
      namesEn.merge(qid, text(props.path("name_en")), (old, neu) -> old.isEmpty() ? neu : old);
    }

    Map<String, NaturalEarthRiver> result = new LinkedHashMap<>();
    lines.forEach(
        (qid, parts) ->
            result.put(qid, new NaturalEarthRiver(qid, names.get(qid), namesEn.get(qid), parts)));
    return result;
  }

  private static List<double[]> line(JsonNode coordinates) {
    List<double[]> points = new ArrayList<>();
    for (JsonNode c : coordinates) {
      points.add(new double[] {c.get(0).asDouble(), c.get(1).asDouble()});
    }
    return points;
  }

  private static String text(JsonNode node) {
    return node == null || node.isNull() || node.isMissingNode() ? "" : node.asString();
  }
}
