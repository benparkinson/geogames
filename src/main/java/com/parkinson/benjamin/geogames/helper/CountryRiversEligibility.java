package com.parkinson.benjamin.geogames.helper;

import com.parkinson.benjamin.geogames.model.CountryRiversIndexEntry;
import java.util.function.Predicate;

/**
 * Which countries the Country Rivers game can ask about. Shared by the game and by the offline
 * rivers program's summary, so both agree.
 */
public final class CountryRiversEligibility {

  /** With fewer rivers the map is a few lines, too little pattern to recognise a country from. */
  public static final int MIN_RIVERS = 5;

  /**
   * Under about a thousand kilometres of river the lines are specks on a map of a whole country.
   */
  public static final double MIN_RIVER_KM = 1000;

  private CountryRiversEligibility() {}

  public static Predicate<CountryRiversIndexEntry> countryRivers() {
    return entry -> entry.riverCount() >= MIN_RIVERS && entry.riverKm() >= MIN_RIVER_KM;
  }
}
