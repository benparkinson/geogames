import type { Country } from "../common/country";

export interface TripointModel {
  coordinate: TripointCoordinate;
  countries: Country[];
}

/** The tripoint plus the order in which hints reveal its countries (by name). */
export interface TripointRoundData extends TripointModel {
  hintOrder: string[];
}

export interface TripointCoordinate {
  latitude: number;
  longitude: number;
}
