package com.parkinson.benjamin.geogames.model;

import com.parkinson.benjamin.geogames.model.geojson.GeoData;

/**
 * The rivers and lakes inside one country, from {@code processed/country-rivers/<country>.json}.
 * Unlike the country shapes this is never held in memory: it is read from its file each time a round
 * is built.
 *
 * @param major MultiLineString: the most important rivers, clipped at the country's borders
 * @param minor MultiLineString: the others
 * @param lakes MultiPolygon: the lakes on the country's shore (each whole, even if it is shared), no
 *     polygons when there are none
 */
public record CountryRivers(String name, GeoData major, GeoData minor, GeoData lakes) {}
