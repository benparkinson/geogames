package com.parkinson.benjamin.geogames.loaders.tripoint;

import com.parkinson.benjamin.geogames.helper.TripointHelper;
import com.parkinson.benjamin.geogames.model.Coordinate;
import com.parkinson.benjamin.geogames.model.Country;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Moves an approximate tripoint coordinate onto the vertex where the borders of the three countries
 * actually meet in our geometry.
 */
public class TripointSnapper {

  static final double SEARCH_WINDOW_DEGREES = 0.5;
  static final double JUNCTION_TOLERANCE_KM = 0.05;
  static final double MAX_SNAP_DISTANCE_KM = 30;

  public Optional<Coordinate> snap(Coordinate original, List<Country> countries) {
    if (countries.size() != 3) {
      return Optional.empty();
    }

    List<Coordinate> a = nearbyVertices(original, countries.get(0));
    List<Coordinate> b = nearbyVertices(original, countries.get(1));
    List<Coordinate> c = nearbyVertices(original, countries.get(2));
    if (a.isEmpty() || b.isEmpty() || c.isEmpty()) {
      return Optional.empty();
    }

    return a.stream()
        .filter(
            vertex ->
                nearestDistanceKm(vertex, b) <= JUNCTION_TOLERANCE_KM
                    && nearestDistanceKm(vertex, c) <= JUNCTION_TOLERANCE_KM)
        .min(Comparator.comparingDouble(vertex -> distanceKm(vertex, original)))
        .filter(vertex -> distanceKm(vertex, original) <= MAX_SNAP_DISTANCE_KM);
  }

  public static double distanceKm(Coordinate p, Coordinate q) {
    double meanLat = Math.toRadians((p.latitude() + q.latitude()) / 2);
    double dx = (p.longitude() - q.longitude()) * 111.32 * Math.cos(meanLat);
    double dy = (p.latitude() - q.latitude()) * 110.57;
    return Math.hypot(dx, dy);
  }

  private static double nearestDistanceKm(Coordinate vertex, List<Coordinate> candidates) {
    return candidates.stream()
        .mapToDouble(candidate -> distanceKm(vertex, candidate))
        .min()
        .orElse(Double.MAX_VALUE);
  }

  private static List<Coordinate> nearbyVertices(Coordinate centre, Country country) {
    return TripointHelper.getCoordinates(country.geoData().geometry()).stream()
        .filter(
            v ->
                Math.abs(v.latitude() - centre.latitude()) <= SEARCH_WINDOW_DEGREES
                    && Math.abs(v.longitude() - centre.longitude()) <= SEARCH_WINDOW_DEGREES)
        .toList();
  }
}
