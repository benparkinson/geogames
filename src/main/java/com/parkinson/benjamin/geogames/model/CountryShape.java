package com.parkinson.benjamin.geogames.model;

import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import java.util.List;

/**
 * Simplified display geometry for one country, from {@code processed/country-shapes}.
 *
 * @param neighbours longest shared border first, ties by name
 * @param focus bounding box of the largest polygon part
 * @param outline simplified Polygon / MultiPolygon
 * @param coast MultiLineString, may have no lines
 * @param landBorders MultiLineString, may have no lines
 */
public record CountryShape(
    String name,
    List<String> additionalNames,
    double coastKm,
    double landBorderKm,
    List<Neighbour> neighbours,
    Bounds focus,
    GeoData outline,
    GeoData coast,
    GeoData landBorders) {

  /** @param mainland whether it touches the country's largest polygon part */
  public record Neighbour(String name, double km, boolean mainland) {}
}
