package com.parkinson.benjamin.geogames.helper;

public final class CountryHelper {

  private CountryHelper() {}

  public static String convertCountryNameToFileName(String countryName) {
    return countryName.replaceAll(" ", "").replaceAll("\\.", "") + ".json";
  }
}
