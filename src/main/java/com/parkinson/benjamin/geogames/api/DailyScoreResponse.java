package com.parkinson.benjamin.geogames.api;

import java.time.LocalDate;
import java.util.List;

/**
 * One day's totals for the daily challenge. {@code averageScore} is rounded to one decimal place,
 * and null when no one has finished. {@code buckets} always has 11 entries: tenths of {@code
 * maxScore}, the last one a perfect score. Nothing here is about one player.
 */
public record DailyScoreResponse(
    LocalDate date, int players, Double averageScore, int maxScore, List<Integer> buckets) {}
