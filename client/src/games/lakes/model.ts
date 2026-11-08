import type { CountryGeoData } from "../common/country";

export interface Lake {
  id: string;
  name: string;
  additionalNames: string[];
  continent: string;
  /** A Polygon or MultiPolygon feature. */
  geoData: CountryGeoData;
  /** Names of the countries on its shore, longest shoreline first. */
  countries: string[];
}

/** The data of a round of the lake game. */
export interface LakeRound {
  lake: Lake;
}
