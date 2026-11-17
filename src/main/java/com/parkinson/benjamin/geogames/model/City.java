package com.parkinson.benjamin.geogames.model;

/**
 * One city of a country, from {@code processed/cities.json}. Only primitives and a name, so the few
 * thousand the app holds in memory take well under a megabyte.
 *
 * @param population the largest figure Natural Earth has for the city or its urban area
 * @param capital whether it is the country's capital
 */
public record City(String name, double lon, double lat, int population, boolean capital) {}
