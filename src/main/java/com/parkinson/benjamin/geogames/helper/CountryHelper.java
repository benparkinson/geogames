package com.parkinson.benjamin.geogames.helper;

import java.text.Normalizer;

public final class CountryHelper {

  private CountryHelper() {}

  /** Plain ASCII, so a file name doesn't depend on the locale of the machine building the jar. */
  public static String convertCountryNameToFileName(String countryName) {
    String withoutAccents =
        Normalizer.normalize(countryName, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    return withoutAccents.replaceAll("[ .']", "") + ".json";
  }
}
