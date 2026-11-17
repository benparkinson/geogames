package com.parkinson.benjamin.geogames.helper;

import java.text.Normalizer;
import java.util.Locale;

/** Place name comparison shared by the app and the offline loaders. */
public final class PlaceNames {

  private PlaceNames() {}

  /** A Java copy of the frontend's {@code normaliseString}. */
  public static String normalise(String s) {
    return Normalizer.normalize(s, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9]", "");
  }
}
