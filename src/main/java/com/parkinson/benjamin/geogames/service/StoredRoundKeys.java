package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.model.CountryPairRound;
import com.parkinson.benjamin.geogames.model.SubjectIds;
import com.parkinson.benjamin.geogames.model.Tripoint;
import java.util.Arrays;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Brings a stored round key up to date after a country is renamed. Games in progress and pinned
 * daily challenges keep the key they were created with, and a key that names the old country would
 * otherwise no longer find its round.
 */
final class StoredRoundKeys {

  private static final String PAIR = "pair:";
  private static final String TRIPOINT = "tripoint:";

  private StoredRoundKeys() {}

  /** The key with each country's current name. A key that names no country is returned as is. */
  static String withCurrentCountryNames(String roundKey, UnaryOperator<String> currentName) {
    if (roundKey.startsWith(SubjectIds.COUNTRY)) {
      return SubjectIds.country(currentName.apply(roundKey.substring(SubjectIds.COUNTRY.length())));
    }
    if (roundKey.startsWith(PAIR)) {
      List<String> names = names(roundKey.substring(PAIR.length()), currentName);
      return names.size() == 2 ? CountryPairRound.subjectId(names.get(0), names.get(1)) : roundKey;
    }
    int coordinate = roundKey.indexOf('@');
    if (roundKey.startsWith(TRIPOINT) && coordinate >= 0) {
      String countries = roundKey.substring(TRIPOINT.length(), coordinate);
      return Tripoint.subjectId(names(countries, currentName)) + roundKey.substring(coordinate);
    }
    return roundKey;
  }

  private static List<String> names(String joined, UnaryOperator<String> currentName) {
    return Arrays.stream(joined.split("\\|", -1)).map(currentName).toList();
  }
}
