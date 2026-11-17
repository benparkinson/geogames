package com.parkinson.benjamin.geogames.helper;

import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import java.util.ArrayList;
import java.util.List;

/**
 * The position of a country for distance clues: the area-weighted centroid of the outer ring of its
 * largest polygon part. Using only the largest part keeps France in Europe rather than pulling it
 * towards French Guiana. The centroid of a crescent-shaped country can fall outside it, which does
 * not matter for a clue measured in hundreds of kilometres.
 */
public final class CountryCentroid {

  private CountryCentroid() {}

  /**
   * @param outline a Polygon or MultiPolygon
   * @return {@code {lat, lon}}, rounded to 2 decimal places
   */
  public static double[] of(GeoData outline) {
    List<double[]> best = null;
    double bestArea = -1;
    for (List<double[]> ring : outerRings(outline)) {
      double area = Math.abs(scaledArea(ring));
      if (area > bestArea) {
        bestArea = area;
        best = ring;
      }
    }
    if (best == null || best.isEmpty()) {
      throw new IllegalArgumentException("Outline has no polygon");
    }
    return centroid(best);
  }

  @SuppressWarnings("unchecked")
  private static List<List<double[]>> outerRings(GeoData outline) {
    List<?> coordinates = outline.geometry().coordinates();
    List<List<List<Number>>> polygons = new ArrayList<>();
    if (outline.geometry().type().equals("Polygon")) {
      polygons.add((List<List<Number>>) (List<?>) coordinates);
    } else {
      for (Object polygon : coordinates) {
        polygons.add((List<List<Number>>) polygon);
      }
    }
    List<List<double[]>> rings = new ArrayList<>();
    for (Object polygon : polygons) {
      List<?> polygonRings = (List<?>) polygon;
      if (polygonRings.isEmpty()) {
        continue;
      }
      List<double[]> ring = new ArrayList<>();
      for (Object p : (List<?>) polygonRings.get(0)) {
        List<Number> position = (List<Number>) p;
        ring.add(new double[] {position.get(0).doubleValue(), position.get(1).doubleValue()});
      }
      rings.add(ring);
    }
    return rings;
  }

  /** Shoelace area with longitudes scaled by cos(mean latitude): only good for comparing parts. */
  private static double scaledArea(List<double[]> ring) {
    double meanLat = ring.stream().mapToDouble(p -> p[1]).average().orElse(0);
    double scale = Math.cos(Math.toRadians(meanLat));
    double sum = 0;
    for (int i = 0; i < ring.size(); i++) {
      double[] a = ring.get(i);
      double[] b = ring.get((i + 1) % ring.size());
      sum += a[0] * scale * b[1] - b[0] * scale * a[1];
    }
    return sum / 2;
  }

  private static double[] centroid(List<double[]> ring) {
    double area2 = 0;
    double cx = 0;
    double cy = 0;
    for (int i = 0; i < ring.size(); i++) {
      double[] a = ring.get(i);
      double[] b = ring.get((i + 1) % ring.size());
      double cross = a[0] * b[1] - b[0] * a[1];
      area2 += cross;
      cx += (a[0] + b[0]) * cross;
      cy += (a[1] + b[1]) * cross;
    }
    double lon;
    double lat;
    if (Math.abs(area2) < 1e-12) {
      lon = ring.stream().mapToDouble(p -> p[0]).average().orElse(0);
      lat = ring.stream().mapToDouble(p -> p[1]).average().orElse(0);
    } else {
      lon = cx / (3 * area2);
      lat = cy / (3 * area2);
    }
    return new double[] {round2(lat), round2(lon)};
  }

  private static double round2(double v) {
    return Math.round(v * 100) / 100.0;
  }
}
