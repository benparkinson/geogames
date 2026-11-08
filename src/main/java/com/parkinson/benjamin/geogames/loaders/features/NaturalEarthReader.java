package com.parkinson.benjamin.geogames.loaders.features;

import com.parkinson.benjamin.geogames.helper.GeoJson;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import tools.jackson.databind.JsonNode;

/** Reads a Natural Earth file (lines or polygons) and groups its features by Wikidata ID. */
public class NaturalEarthReader {

  public Map<String, NaturalEarthFeature> read(Path file, Set<String> skippedClasses)
      throws IOException {
    try (InputStream in = Files.newInputStream(file)) {
      return group(GeoJson.lenientReader().readTree(in), skippedClasses);
    }
  }

  /**
   * Groups features by {@code wikidataid}. Features without one are skipped, and so are those
   * whose {@code featurecla} is in {@code skippedClasses}. Rivers keep lake centerlines, because
   * they keep a river continuous through a lake.
   */
  public Map<String, NaturalEarthFeature> group(
      JsonNode featureCollection, Set<String> skippedClasses) {
    Map<String, String> names = new LinkedHashMap<>();
    Map<String, String> namesEn = new LinkedHashMap<>();
    Map<String, List<List<double[]>>> lines = new LinkedHashMap<>();
    Map<String, List<List<List<double[]>>>> polygons = new LinkedHashMap<>();

    for (JsonNode feature : featureCollection.path("features")) {
      JsonNode props = feature.path("properties");
      String qid = text(props.path("wikidataid"));
      if (qid.isEmpty() || skippedClasses.contains(text(props.path("featurecla")))) {
        continue;
      }
      List<List<double[]>> parts = lines.computeIfAbsent(qid, k -> new ArrayList<>());
      List<List<List<double[]>>> shapes = polygons.computeIfAbsent(qid, k -> new ArrayList<>());
      JsonNode geometry = feature.path("geometry");
      String type = text(geometry.path("type"));
      if (type.equals("LineString")) {
        parts.add(line(geometry.path("coordinates")));
      } else if (type.equals("MultiLineString")) {
        for (JsonNode part : geometry.path("coordinates")) {
          parts.add(line(part));
        }
      } else if (type.equals("Polygon")) {
        shapes.add(polygon(geometry.path("coordinates")));
      } else if (type.equals("MultiPolygon")) {
        for (JsonNode shape : geometry.path("coordinates")) {
          shapes.add(polygon(shape));
        }
      }
      names.merge(qid, text(props.path("name")), (old, neu) -> old.isEmpty() ? neu : old);
      namesEn.merge(qid, text(props.path("name_en")), (old, neu) -> old.isEmpty() ? neu : old);
    }

    Map<String, NaturalEarthFeature> result = new LinkedHashMap<>();
    lines.forEach(
        (qid, parts) ->
            result.put(
                qid,
                new NaturalEarthFeature(
                    qid, names.get(qid), namesEn.get(qid), parts, polygons.get(qid))));
    return result;
  }

  private static List<List<double[]>> polygon(JsonNode rings) {
    List<List<double[]>> result = new ArrayList<>();
    for (JsonNode ring : rings) {
      result.add(line(ring));
    }
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
