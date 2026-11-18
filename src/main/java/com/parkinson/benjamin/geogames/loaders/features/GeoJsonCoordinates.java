package com.parkinson.benjamin.geogames.loaders.features;

import java.util.ArrayList;
import java.util.List;
import tools.jackson.databind.JsonNode;

/** Reads the coordinates of a GeoJSON geometry node into lists of {lon, lat} points. */
public final class GeoJsonCoordinates {

  private GeoJsonCoordinates() {}

  public static List<List<double[]>> lines(JsonNode geometry) {
    List<List<double[]>> lines = new ArrayList<>();
    String type = geometry.path("type").asString("");
    if (type.equals("LineString")) {
      lines.add(points(geometry.path("coordinates")));
    } else if (type.equals("MultiLineString")) {
      for (JsonNode line : geometry.path("coordinates")) {
        lines.add(points(line));
      }
    }
    return lines;
  }

  public static List<List<List<double[]>>> polygons(JsonNode geometry) {
    List<List<List<double[]>>> polygons = new ArrayList<>();
    String type = geometry.path("type").asString("");
    if (type.equals("Polygon")) {
      polygons.add(rings(geometry.path("coordinates")));
    } else if (type.equals("MultiPolygon")) {
      for (JsonNode polygon : geometry.path("coordinates")) {
        polygons.add(rings(polygon));
      }
    }
    return polygons;
  }

  public static List<List<double[]>> rings(JsonNode rings) {
    List<List<double[]>> result = new ArrayList<>();
    for (JsonNode ring : rings) {
      result.add(points(ring));
    }
    return result;
  }

  public static List<double[]> points(JsonNode line) {
    List<double[]> points = new ArrayList<>();
    for (JsonNode p : line) {
      points.add(new double[] {p.get(0).asDouble(), p.get(1).asDouble()});
    }
    return points;
  }
}
