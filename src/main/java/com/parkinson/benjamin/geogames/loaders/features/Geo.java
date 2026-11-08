package com.parkinson.benjamin.geogames.loaders.features;

import java.util.List;

/** Great-circle distances on points given as {lon, lat}. */
public final class Geo {

  public static final double EARTH_RADIUS_KM = 6371.0088;

  private Geo() {}

  public static double haversineKm(double[] a, double[] b) {
    double lat1 = Math.toRadians(a[1]);
    double lat2 = Math.toRadians(b[1]);
    double dLat = lat2 - lat1;
    double dLon = Math.toRadians(b[0] - a[0]);
    double h =
        Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
    return 2 * EARTH_RADIUS_KM * Math.asin(Math.min(1, Math.sqrt(h)));
  }

  public static double lineKm(List<double[]> line) {
    double km = 0;
    for (int i = 1; i < line.size(); i++) {
      km += haversineKm(line.get(i - 1), line.get(i));
    }
    return km;
  }
}
