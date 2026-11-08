package com.parkinson.benjamin.geogames.model;

import com.parkinson.benjamin.geogames.model.geojson.GeoData;

/** Just a neighbour's name and outline: the country-shape games draw nothing else of it. */
public record CountryOutline(String name, GeoData outline) {}
