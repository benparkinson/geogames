package com.parkinson.benjamin.geogames.loaders.features;

import java.util.List;

/**
 * All Natural Earth features that share one Wikidata ID. Points are {lon, lat}.
 *
 * @param lines from LineString / MultiLineString features
 * @param polygons from Polygon / MultiPolygon features: each polygon is a list of rings, the first
 *     being the outer ring
 */
public record NaturalEarthFeature(
    String qid,
    String name,
    String nameEn,
    List<List<double[]>> lines,
    List<List<List<double[]>>> polygons) {}
