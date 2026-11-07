import type { Coordinate, Country } from "../common/country";

export interface TripointModel {
  coordinate: Coordinate;
  countries: Country[];
}

/** The tripoint plus the order in which hints reveal its countries (by name). */
export interface TripointRoundData extends TripointModel {
  hintOrder: string[];
}
