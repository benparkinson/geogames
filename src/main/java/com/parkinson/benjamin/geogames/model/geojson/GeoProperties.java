package com.parkinson.benjamin.geogames.model.geojson;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GeoProperties(@JsonProperty("ADMIN") String name) {
}
