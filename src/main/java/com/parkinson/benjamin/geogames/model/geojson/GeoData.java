package com.parkinson.benjamin.geogames.model.geojson;

public record GeoData(String type, GeoProperties properties, GeoGeometry geometry) {

  /** A GeoJSON feature with this name and geometry. */
  public static GeoData feature(String name, GeoGeometry geometry) {
    return new GeoData("Feature", new GeoProperties(name), geometry);
  }
}
