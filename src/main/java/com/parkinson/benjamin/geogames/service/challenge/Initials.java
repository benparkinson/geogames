package com.parkinson.benjamin.geogames.service.challenge;

import com.parkinson.benjamin.geogames.api.BadRequestException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Arcade-style initials for a challenge's league table: one to three letters, A to Z. The only free
 * text the server keeps, and shown to anyone with the link, hence the blocklist. More words can be
 * blocked without a release: set {@code BLOCKED_INITIALS} (comma separated) and restart.
 */
@Component
public class Initials {

  private static final Pattern LETTERS = Pattern.compile("[A-Za-z]{1,3}");

  private static final Set<String> BUILT_IN =
      Set.of(
          "ASS", "CNT", "COK", "CUM", "DIK", "FAG", "FCK", "FGT", "FUC", "FUK", "FUQ", "KKK", "KYS",
          "NGA", "NGR", "NIG", "PIS", "TIT", "WOG");

  private final Set<String> blocked = new HashSet<>(BUILT_IN);

  public Initials(@Value("${geogames.challenges.blocked-initials}") String[] extra) {
    Arrays.stream(extra).map(word -> word.strip().toUpperCase(Locale.ROOT)).forEach(blocked::add);
  }

  /**
   * The initials trimmed and upper-cased.
   *
   * @throws BadRequestException if they are missing, not one to three letters, or blocked
   */
  public String normalise(String initials) {
    String trimmed = initials == null ? "" : initials.strip();
    // Matched before upper-casing, which turns some other characters into A-Z (a sharp s into SS).
    if (!LETTERS.matcher(trimmed).matches()) {
      throw new BadRequestException("Initials must be one to three letters, A to Z");
    }
    String upper = trimmed.toUpperCase(Locale.ROOT);
    if (blocked.contains(upper)) {
      throw new BadRequestException("Choose different initials");
    }
    return upper;
  }

  /** Stored initials as they may be shown: null if they have been blocked since they were set. */
  public String visible(String stored) {
    return stored == null || blocked.contains(stored) ? null : stored;
  }
}
