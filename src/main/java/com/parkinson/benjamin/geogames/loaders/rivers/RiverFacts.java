package com.parkinson.benjamin.geogames.loaders.rivers;

import com.parkinson.benjamin.geogames.model.Coordinate;
import java.util.List;

/**
 * What the loader works out about a river beyond its line: where it starts and ends, how sure it
 * is of the direction, and the countries it flows through.
 */
public record RiverFacts(
    Coordinate source, Coordinate mouth, double flowConfidence, List<String> countries) {

  /** Computes the facts, letting the river's override win over what was computed. */
  public static RiverFacts compute(
      List<List<double[]>> lines, RiverOverride override, RiverCountryLocator.Result located) {
    RiverEnds.Ends ends = RiverEnds.find(lines);
    double[] source = ends.source();
    double[] mouth = ends.mouth();
    if (override != null && Boolean.TRUE.equals(override.reverseFlow())) {
      double[] swap = source;
      source = mouth;
      mouth = swap;
    }
    Coordinate sourceCoordinate = new Coordinate(source[1], source[0]);
    Coordinate mouthCoordinate = new Coordinate(mouth[1], mouth[0]);
    if (override != null && override.source() != null) {
      sourceCoordinate = override.source();
    }
    if (override != null && override.mouth() != null) {
      mouthCoordinate = override.mouth();
    }
    return new RiverFacts(
        sourceCoordinate, mouthCoordinate, ends.confidence(), located.countries());
  }
}
