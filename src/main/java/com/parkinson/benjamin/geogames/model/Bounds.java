package com.parkinson.benjamin.geogames.model;

import com.parkinson.benjamin.geogames.model.geojson.GeoData;

/** A bounding box in degrees. */
public record Bounds(double west, double south, double east, double north) {

  /** The smallest box holding both this one and {@code other}. */
  public Bounds union(Bounds other) {
    return new Bounds(
        Math.min(west, other.west),
        Math.min(south, other.south),
        Math.max(east, other.east),
        Math.max(north, other.north));
  }

  /** The part both boxes share; only meaningful when they overlap. */
  public Bounds intersect(Bounds other) {
    return new Bounds(
        Math.max(west, other.west),
        Math.max(south, other.south),
        Math.min(east, other.east),
        Math.min(north, other.north));
  }

  /** This box grown on each side by {@code share} of its width (east, west) or height. */
  public Bounds expand(double share) {
    double dx = (east - west) * share;
    double dy = (north - south) * share;
    return new Bounds(west - dx, south - dy, east + dx, north + dy);
  }

  /** Width times height in degrees: a rough size for comparing boxes, not a true area. */
  public double area() {
    return (east - west) * (north - south);
  }

  /** The box round every position of the geometry. */
  public static Bounds of(GeoData data) {
    // west, south, east, north
    double[] box = {Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
    data.geometry()
        .forEachPosition(
            (lon, lat) -> {
              box[0] = Math.min(box[0], lon);
              box[1] = Math.min(box[1], lat);
              box[2] = Math.max(box[2], lon);
              box[3] = Math.max(box[3], lat);
            });
    return new Bounds(box[0], box[1], box[2], box[3]);
  }
}
