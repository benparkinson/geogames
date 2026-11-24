package com.parkinson.benjamin.geogames.helper;

import com.parkinson.benjamin.geogames.model.River;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Which rivers the Follow the River game can ask about. Shared by the game and by {@code
 * ProcessedDataIT}, so both agree.
 */
public final class RiverCountriesEligibility {

  /**
   * One outline is a country, not a river; two is enough when the pair names one river.
   */
  public static final int MIN_COUNTRIES = 2;

  /**
   * Rivers the game never asks about. The outlines are all the player sees, so two eligible rivers
   * must never share a country set. The Columbia and the Yukon both cross only the United States
   * and Canada, so the outlines can't tell them apart (or from any other river on that border).
   * The Tigris has the Euphrates' countries (Iraq, Türkiye, Syria), and the Euphrates is the
   * longer and better known. River ids are Wikidata QIDs.
   */
  public static final Set<String> EXCLUDED = Set.of("Q2251", "Q104437", "Q35591");

  private RiverCountriesEligibility() {}

  public static Predicate<River> riverCountries() {
    return river ->
        river.countries() != null
            && river.countries().size() >= MIN_COUNTRIES
            && !EXCLUDED.contains(river.id());
  }
}
