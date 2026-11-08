package com.parkinson.benjamin.geogames.helper;

import com.parkinson.benjamin.geogames.model.CountryShape;
import java.util.function.Predicate;

/**
 * Which countries each country-shape game can ask about. Shared by the games and by the offline
 * shape program's summary, so both agree.
 */
public final class CountryShapeEligibility {

  /** Fewer neighbours leave too little of a gap to guess from. */
  public static final int NEIGHBOURS_MIN_MAINLAND = 2;

  /** Under 150 km a coast is just a short line (Iraq 91, Belgium 95). */
  public static final double COASTLINE_MIN_COAST_KM = 150;

  /** Without land borders the coast is the whole outline, which is a different, easier game. */
  public static final double COASTLINE_MIN_LAND_KM = 50;

  /** Too short a border is a few lines nobody can recognise. */
  public static final double LAND_BORDERS_MIN_LAND_KM = 300;

  /** Landlocked countries are left out: their "land borders only" is the whole outline. */
  public static final double LAND_BORDERS_MIN_COAST_KM = 50;

  private CountryShapeEligibility() {}

  public static Predicate<CountryShape> neighbours() {
    return shape ->
        shape.neighbours().stream().filter(CountryShape.Neighbour::mainland).count()
            >= NEIGHBOURS_MIN_MAINLAND;
  }

  public static Predicate<CountryShape> coastline() {
    return shape ->
        shape.coastKm() >= COASTLINE_MIN_COAST_KM && shape.landBorderKm() >= COASTLINE_MIN_LAND_KM;
  }

  /**
   * The game draws the mainland neighbours' outlines around the borders, so a country whose only
   * neighbours touch an outlying part (the United Kingdom, bordering Ireland only in Northern
   * Ireland) would show no context at all.
   */
  public static Predicate<CountryShape> landBorders() {
    return shape ->
        shape.landBorderKm() >= LAND_BORDERS_MIN_LAND_KM
            && shape.coastKm() >= LAND_BORDERS_MIN_COAST_KM
            && shape.neighbours().stream().anyMatch(CountryShape.Neighbour::mainland);
  }
}
