package com.parkinson.benjamin.geogames.loaders.features;

import java.util.List;

/** Great-circle distances on points given as {lon, lat}. */
public final class Geo {

  public static final double EARTH_RADIUS_KM = 6371.0088;

  /** The one country with no shape: the games and the loaders skip it. */
  public static final String ANTARCTICA = "Antarctica";

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

  /**
   * The area in km² of the polygon a ring of {lon, lat} points (in degrees) encloses, on a sphere.
   * It is the absolute value, so the winding doesn't matter, and the ring may be open or closed. An
   * edge across the antimeridian counts the short way round.
   *
   * <p>Per edge, with longitudes λ and latitudes φ in radians:
   * (λ₂ − λ₁) · (2 + sin φ₁ + sin φ₂), summed, times R² / 2.
   */
  public static double ringAreaKm2(List<double[]> ring) {
    double sum = 0;
    int n = ring.size();
    for (int i = 0; i < n; i++) {
      double[] a = ring.get(i);
      double[] b = ring.get((i + 1) % n);
      double dLon = Math.toRadians(b[0] - a[0]);
      if (dLon > Math.PI) {
        dLon -= 2 * Math.PI;
      } else if (dLon < -Math.PI) {
        dLon += 2 * Math.PI;
      }
      sum += dLon * (2 + Math.sin(Math.toRadians(a[1])) + Math.sin(Math.toRadians(b[1])));
    }
    return Math.abs(sum) * EARTH_RADIUS_KM * EARTH_RADIUS_KM / 2;
  }

  public static double lineKm(List<double[]> line) {
    double km = 0;
    for (int i = 1; i < line.size(); i++) {
      km += haversineKm(line.get(i - 1), line.get(i));
    }
    return km;
  }
}
