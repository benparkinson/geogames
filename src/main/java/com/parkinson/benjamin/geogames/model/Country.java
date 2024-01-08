package com.parkinson.benjamin.geogames.model;

import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import java.util.List;

public record Country(String name, List<String> additionalNames, GeoData geoData) {}
