package com.parkinson.benjamin.geogames.loaders.features;

import java.text.Normalizer;
import java.util.Locale;

/** File name slugs. */
public final class Slugs {

  private Slugs() {}

  /** Lower-case ASCII words joined by hyphens, or {@code fallback} if nothing is left. */
  public static String slug(String name, String fallback) {
    String slug =
        Normalizer.normalize(name, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("^-|-$", "");
    return slug.isEmpty() ? fallback : slug;
  }
}
