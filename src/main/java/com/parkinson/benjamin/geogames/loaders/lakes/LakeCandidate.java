package com.parkinson.benjamin.geogames.loaders.lakes;

import com.parkinson.benjamin.geogames.loaders.features.FeatureSelector;
import java.util.List;

/**
 * A lake with a name, simplified polygons and an area. The continent is null when it couldn't be
 * resolved.
 */
public record LakeCandidate(
    String qid,
    String name,
    List<String> aliases,
    String continent,
    int sitelinks,
    boolean fromOverride,
    List<List<List<double[]>>> polygons,
    double areaKm2)
    implements FeatureSelector.RankedFeature {}
