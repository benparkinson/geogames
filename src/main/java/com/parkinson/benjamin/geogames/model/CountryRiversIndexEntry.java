package com.parkinson.benjamin.geogames.model;

/**
 * One line of {@code processed/country-rivers/index.json}: what is in a country's rivers file,
 * without its geometry, so the app can choose countries without reading any.
 *
 * @param file the name of the country's file in the same folder
 * @param riverCount how many different rivers have a line in the country
 * @param riverKm the length of all those lines, in whole kilometres
 * @param lakeCount how many lakes are on the country's shore
 */
public record CountryRiversIndexEntry(
    String name, String file, int riverCount, long riverKm, int lakeCount) {}
