package com.parkinson.benjamin.geogames.model;

import java.util.List;

/**
 * A country for autocomplete and distance clues.
 *
 * @param lat latitude of the country's position: the area-weighted centroid of the outer ring of
 *     its largest polygon part (so France is in Europe, not pulled towards French Guiana)
 * @param lon longitude of the same position
 */
public record CountryName(String name, List<String> additionalNames, double lat, double lon) {}
