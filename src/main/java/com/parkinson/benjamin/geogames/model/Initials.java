package com.parkinson.benjamin.geogames.model;

import com.parkinson.benjamin.geogames.api.BadRequestException;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Arcade-style initials for a challenge's league table: one to three letters, A to Z. The only free
 * text the server keeps, and shown to anyone with the link, hence the blocklist.
 */
public final class Initials {

  private static final Pattern LETTERS = Pattern.compile("[A-Za-z]{1,3}");

  private static final Set<String> BLOCKED =
      Set.of(
          "ASS", "CNT", "COK", "CUM", "DIK", "FAG", "FCK", "FUC", "FUK", "FUQ", "KKK", "KYS", "NGA",
          "NGR", "NIG", "PIS", "TIT", "WOG");

  private Initials() {}

  /**
   * The initials trimmed and upper-cased.
   *
   * @throws BadRequestException if they are missing, not one to three letters, or blocked
   */
  public static String normalise(String initials) {
    String trimmed = initials == null ? "" : initials.strip();
    // Matched before upper-casing, which turns some other characters into A-Z (a sharp s into SS).
    if (!LETTERS.matcher(trimmed).matches()) {
      throw new BadRequestException("Initials must be one to three letters, A to Z");
    }
    String upper = trimmed.toUpperCase(Locale.ROOT);
    if (BLOCKED.contains(upper)) {
      throw new BadRequestException("Choose different initials");
    }
    return upper;
  }
}
