package com.parkinson.benjamin.geogames.loaders.rivers;

import com.parkinson.benjamin.geogames.loaders.features.AliasBuilder;
import com.parkinson.benjamin.geogames.model.Coordinate;
import java.util.List;

/**
 * One hand-written entry of {@code overrides.json}, keyed by Wikidata QID. All fields optional.
 *
 * <p>{@code mainPieceOnly} keeps only the river's main connected piece and drops lines that are
 * detached from it (see {@link RiverGeometry#mainPiece}), for rivers where Natural Earth tags a
 * far-off stretch with the same QID. {@code reverseFlow} swaps the computed source and mouth, for
 * rivers digitised upstream. {@code source} and {@code mouth} replace the computed ends outright.
 */
public record RiverOverride(
    Boolean exclude,
    String name,
    String continent,
    List<String> aliases,
    List<String> addAliases,
    List<String> removeAliases,
    Boolean mainPieceOnly,
    Boolean reverseFlow,
    Coordinate source,
    Coordinate mouth)
    implements AliasBuilder.NamedOverride {}
