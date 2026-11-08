package com.parkinson.benjamin.geogames.loaders.rivers;

import com.parkinson.benjamin.geogames.loaders.features.FeatureSelector;
import java.util.List;

/**
 * A river with a name, simplified geometry and a length. The continent is null when it couldn't be
 * resolved.
 */
public record RiverCandidate(
    String qid,
    String name,
    List<String> aliases,
    String continent,
    int sitelinks,
    boolean fromOverride,
    List<List<double[]>> lines,
    double km)
    implements FeatureSelector.RankedFeature {}
