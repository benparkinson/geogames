package com.parkinson.benjamin.geogames.loaders.rivers;

import java.util.List;

/** All Natural Earth line parts that share one Wikidata ID. Points are {lon, lat}. */
public record NaturalEarthRiver(
    String qid, String name, String nameEn, List<List<double[]>> lines) {}
