package com.parkinson.benjamin.geogames.model;

import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import java.util.List;

/**
 * A lake, as stored in {@code processed/lakes}. Rounds wrap it in a {@link LakeRound}.
 *
 * @param geoData a Feature with a Polygon (one polygon) or a MultiPolygon
 * @param countries the names of the countries on its shore, longest shoreline first, exactly as
 *     {@link Country#name()}
 */
public record Lake(
    String id,
    String name,
    List<String> additionalNames,
    String continent,
    GeoData geoData,
    List<String> countries) {}
