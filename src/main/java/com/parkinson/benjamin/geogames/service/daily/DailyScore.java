package com.parkinson.benjamin.geogames.service.daily;

import java.util.List;

/** One day's running totals: finished games, the sum of their scores and the 11 score buckets. */
public record DailyScore(int players, int scoreTotal, List<Integer> buckets) {

  public static final int BUCKETS = 11;

  public static DailyScore empty() {
    return new DailyScore(0, 0, java.util.Collections.nCopies(BUCKETS, 0));
  }
}
