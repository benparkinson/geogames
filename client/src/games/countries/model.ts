import type { CountryGeoData, LineGeoData } from "../common/country";

export interface Bounds {
  west: number;
  south: number;
  east: number;
  north: number;
}

export interface Neighbour {
  name: string;
  /** Length of the shared border, in whole kilometres. */
  km: number;
  /** Whether it touches the country's largest polygon part (not an overseas territory). */
  mainland: boolean;
}

/** A country as the country-shape games get it: simplified display geometry and border facts. */
export interface CountryShape {
  name: string;
  additionalNames: string[];
  coastKm: number;
  landBorderKm: number;
  /** Longest shared border first. */
  neighbours: Neighbour[];
  /** Bounding box of the largest polygon part: what to fit the map to. */
  focus: Bounds;
  outline: CountryGeoData;
  /** May have no lines. */
  coast: LineGeoData;
  /** May have no lines. */
  landBorders: LineGeoData;
}

/** A neighbour's name and outline: all the games draw of it. */
export interface CountryOutline {
  name: string;
  outline: CountryGeoData;
}

/** The data of a round of any country-shape game. `neighbours` is empty for the games that don't show them. */
export interface CountryShapeRound {
  country: CountryShape;
  neighbours: CountryOutline[];
}
