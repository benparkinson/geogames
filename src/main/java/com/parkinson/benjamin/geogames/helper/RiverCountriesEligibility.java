package com.parkinson.benjamin.geogames.helper;

import com.parkinson.benjamin.geogames.model.River;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Which rivers the River Countries game can ask about. Shared by the game and by {@code
 * ProcessedDataIT}, so both agree.
 */
public final class RiverCountriesEligibility {

  /**
   * One outline is a country, not a river; two is enough when the pair names one river.
   */
  public static final int MIN_COUNTRIES = 2;

  /**
   * The Columbia and the Yukon both cross only the United States and Canada, so the two outlines
   * can't tell them apart (or from any other river on that border). River ids are Wikidata QIDs.
   */
  public static final Set<String> EXCLUDED = Set.of("Q2251", "Q104437");

  private RiverCountriesEligibility() {}

  public static Predicate<River> riverCountries() {
    return river ->
        river.countries() != null
            && river.countries().size() >= MIN_COUNTRIES
            && !EXCLUDED.contains(river.id());
  }
}
