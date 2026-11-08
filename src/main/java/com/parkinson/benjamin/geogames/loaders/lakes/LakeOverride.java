package com.parkinson.benjamin.geogames.loaders.lakes;

import com.parkinson.benjamin.geogames.loaders.features.AliasBuilder;
import java.util.List;

/**
 * One hand-written entry of {@code geodata/lakes/overrides.json}, keyed by Wikidata QID. All fields
 * optional. {@code countries} replaces the located countries outright.
 */
public record LakeOverride(
    Boolean exclude,
    String name,
    String continent,
    List<String> aliases,
    List<String> addAliases,
    List<String> removeAliases,
    List<String> countries)
    implements AliasBuilder.NamedOverride {}
