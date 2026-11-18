package com.parkinson.benjamin.geogames;

/** Values shared by tests that depend on the checked-in processed data. */
public final class TestFixtures {

  /**
   * Number of countries in the autocomplete list: the files in {@code processed/country-shapes}
   * (241) less the ones in {@code CountryNameService.NOT_IN_AUTOCOMPLETE}. Update here when the
   * country data changes.
   */
  public static final int EXPECTED_COUNTRY_COUNT = 240;

  private TestFixtures() {}
}
