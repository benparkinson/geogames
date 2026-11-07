package com.parkinson.benjamin.geogames.model;

import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import java.util.List;

/** A river, as stored in {@code processed/rivers}. Rounds wrap it in a {@link RiverRound}. */
public record River(
    String id, String name, List<String> additionalNames, String continent, GeoData geoData) {}
