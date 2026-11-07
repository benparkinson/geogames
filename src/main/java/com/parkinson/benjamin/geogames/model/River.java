package com.parkinson.benjamin.geogames.model;

import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import java.util.List;

/**
 * A river, as stored in {@code processed/rivers}. Rounds wrap it in a {@link RiverRound}.
 *
 * @param source where the river starts
 * @param mouth where the river ends
 * @param countries the names of the countries it flows through, longest stretch first, exactly as
 *     {@link Country#name()}
 */
public record River(
    String id,
    String name,
    List<String> additionalNames,
    String continent,
    GeoData geoData,
    Coordinate source,
    Coordinate mouth,
    List<String> countries) {}
