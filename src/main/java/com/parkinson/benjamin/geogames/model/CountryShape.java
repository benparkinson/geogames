package com.parkinson.benjamin.geogames.model;

import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import java.util.List;

/**
 * Simplified display geometry for one country, from {@code processed/country-shapes}.
 *
 * @param continents one or two of the six continent names, in the order the game shows them
 * @param areaKm2 area of the largest polygon part (outer ring minus holes), at full resolution
 * @param neighbours longest shared border first, ties by name
 * @param focus bounding box of the largest polygon part
 * @param outline simplified Polygon / MultiPolygon
 * @param coast MultiLineString, may have no lines
 * @param landBorders MultiLineString, may have no lines
 */
public record CountryShape(
    String name,
    List<String> additionalNames,
    List<String> continents,
    double coastKm,
    double landBorderKm,
    long areaKm2,
    List<Neighbour> neighbours,
    Bounds focus,
    GeoData outline,
    GeoData coast,
    GeoData landBorders) {

  /** A shape with no continents yet: what the builder makes before the loader adds them. */
  public CountryShape(
      String name,
      List<String> additionalNames,
      double coastKm,
      double landBorderKm,
      long areaKm2,
      List<Neighbour> neighbours,
      Bounds focus,
      GeoData outline,
      GeoData coast,
      GeoData landBorders) {
    this(
        name,
        additionalNames,
        List.of(),
        coastKm,
        landBorderKm,
        areaKm2,
        neighbours,
        focus,
        outline,
        coast,
        landBorders);
  }

  public CountryShape withContinents(List<String> continents) {
    return new CountryShape(
        name,
        additionalNames,
        continents,
        coastKm,
        landBorderKm,
        areaKm2,
        neighbours,
        focus,
        outline,
        coast,
        landBorders);
  }

  /**
   * @param mainland whether it touches the country's largest polygon part
   * @param lines the indexes, ascending, of the lines of {@code landBorders} shared with this
   *     neighbour
   */
  public record Neighbour(String name, double km, boolean mainland, List<Integer> lines) {}
}
