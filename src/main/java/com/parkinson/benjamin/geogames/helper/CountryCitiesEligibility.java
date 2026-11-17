package com.parkinson.benjamin.geogames.helper;

import com.parkinson.benjamin.geogames.model.CountryCities;
import java.util.function.Predicate;

/**
 * Which countries the Country Cities game can ask about. Shared by the game and by the offline city
 * program's summary, so both agree.
 */
public final class CountryCitiesEligibility {

  /**
   * With fewer than eight dots there is no pattern to recognise a country from: the dots could be
   * almost anywhere.
   */
  public static final int MIN_CITIES = 8;

  private CountryCitiesEligibility() {}

  public static Predicate<CountryCities> countryCities() {
    return country -> country.cities().size() >= MIN_CITIES;
  }
}
