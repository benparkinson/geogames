package com.parkinson.benjamin.geogames.model;

/** The prefixes of the subject ids (and round keys) of rounds that are about one item. */
public final class SubjectIds {
  public static final String COUNTRY = "country:";
  public static final String RIVER = "river:";
  public static final String LAKE = "lake:";

  private SubjectIds() {}

  public static String country(String name) {
    return COUNTRY + name;
  }

  public static String river(String id) {
    return RIVER + id;
  }

  public static String lake(String id) {
    return LAKE + id;
  }
}
