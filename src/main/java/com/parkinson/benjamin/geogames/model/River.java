package com.parkinson.benjamin.geogames.model;

import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import java.util.List;

public record River(String name, List<String> additionalNames, String continent, GeoData geoData) {

}
