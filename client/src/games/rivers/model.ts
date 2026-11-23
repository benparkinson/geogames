import type { Coordinate, Country, LineGeoData } from "../common/country";
import type { Bounds } from "../countries/model";

export interface River {
  id: string;
  name: string;
  additionalNames: string[];
  geoData: LineGeoData;
  continent: string;
  /** Where the river starts and ends. */
  source: Coordinate;
  mouth: Coordinate;
  /** Names of the countries it flows through, longest stretch first. */
  countries: string[];
}

/** The data of a round of any river game. `countries` is empty for the games that don't show them. */
export interface RiverRound {
  river: River;
  countries: Country[];
  /** The box the map opens on; null for the games that show no countries. */
  focus?: Bounds | null;
}
