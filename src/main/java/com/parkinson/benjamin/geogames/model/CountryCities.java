package com.parkinson.benjamin.geogames.model;

import java.util.List;

/**
 * The cities kept for one country: its largest, and always its capital. The format of one element
 * of {@code processed/cities.json}.
 *
 * @param cities the largest first, ties by name
 */
public record CountryCities(String country, List<City> cities) {}
