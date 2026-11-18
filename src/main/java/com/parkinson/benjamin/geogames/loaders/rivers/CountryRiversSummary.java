package com.parkinson.benjamin.geogames.loaders.rivers;

import com.parkinson.benjamin.geogames.helper.CountryRiversEligibility;
import com.parkinson.benjamin.geogames.model.CountryRivers;
import com.parkinson.benjamin.geogames.model.CountryRiversIndexEntry;
import java.util.List;
import java.util.Map;

/** Prints what a {@link CountryRiversMain} run produced. */
final class CountryRiversSummary {

  private CountryRiversSummary() {}

  static void printSummary(CountryRiversMain.Report report) {
    double total = report.kmByScalerank().values().stream().mapToDouble(Double::doubleValue).sum();
    System.out.println("Source river length by scalerank (lower is more important):");
    System.out.println("Major: scalerank up to " + CountryRiversMain.MAJOR_MAX_SCALERANK);
    double cumulative = 0;
    for (Map.Entry<Integer, Double> e : report.kmByScalerank().entrySet()) {
      cumulative += e.getValue();
      System.out.printf(
          "  %2d: %8.0f km, %5.1f%% cumulative%n",
          e.getKey(), e.getValue(), 100 * cumulative / total);
    }
    double kept = report.majorKm() + report.minorKm();
    System.out.printf(
        "Kept: %.0f km, of which major %.1f%%%n",
        kept, kept == 0 ? 0 : 100 * report.majorKm() / kept);
    System.out.println(
        "Dropped to stay within "
            + CountryRiverLimits.MAX_RIVER_POINTS
            + " river points: "
            + report.droppedForSize()
            + " stretches; within "
            + CountryLakes.MAX_LAKE_POINTS
            + " lake points: "
            + report.lakesDroppedForSize()
            + " lakes");

    List<CountryRiversIndexEntry> index = report.index();
    System.out.println("Written: " + index.size() + " countries, " + report.bytes() + " bytes");
    CountryRivers largest = null;
    int largestPoints = 0;
    for (CountryRivers rivers : report.rivers().values()) {
      int points =
          rivers.major().geometry().positionCount() + rivers.minor().geometry().positionCount();
      if (points > largestPoints) {
        largestPoints = points;
        largest = rivers;
      }
    }
    if (largest != null) {
      System.out.println("Most river points: " + largest.name() + " " + largestPoints);
    }

    List<CountryRiversIndexEntry> eligible =
        index.stream().filter(CountryRiversEligibility.countryRivers()).toList();
    System.out.println(
        "Eligible (at least "
            + CountryRiversEligibility.MIN_RIVERS
            + " rivers and "
            + (long) CountryRiversEligibility.MIN_RIVER_KM
            + " km): "
            + eligible.size());
    eligible.forEach(
        e ->
            System.out.println(
                "  "
                    + e.name()
                    + ": "
                    + e.riverCount()
                    + " rivers, "
                    + e.riverKm()
                    + " km, "
                    + e.lakeCount()
                    + " lakes"));
  }
}
