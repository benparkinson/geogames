import type { BBox, GeoJsonObject, GeoJsonProperties, MultiLineString } from "geojson";
import type { Coordinate, Country } from "../common/country";

export interface River {
  id: string;
  name: string;
  additionalNames: string[];
  geoData: RiverGeoData;
  continent: string;
  /** Where the river starts and ends. */
  source: Coordinate;
  mouth: Coordinate;
  /** Names of the countries it flows through, longest stretch first. */
  countries: string[];
}

export interface RiverGeoData extends GeoJsonObject {
  type: "MultiLineString";
  bbox?: BBox;
  properties: GeoJsonProperties;
  geometry: MultiLineString;
}

/** The data of a round of any river game. `countries` is empty for the games that don't show them. */
export interface RiverRound {
  river: River;
  countries: Country[];
}
