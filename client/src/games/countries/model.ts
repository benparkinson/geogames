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
  /** Which lines of `landBorders` (by index, ascending) are the border with this neighbour. */
  lines: number[];
}

/** A country as the country-shape games get it: simplified display geometry and border facts. */
export interface CountryShape {
  name: string;
  additionalNames: string[];
  /** One or two continent names; the client ignores them. */
  continents: string[];
  coastKm: number;
  landBorderKm: number;
  /** Area of the largest part, in km². */
  areaKm2: number;
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

/** The data of a round of any game whose answer is one country. */
export interface HasCountry {
  country: CountryShape;
}

/** One city, as Country Cities draws it. */
export interface City {
  name: string;
  lon: number;
  lat: number;
  population: number;
  capital: boolean;
}

/** The data of a round of Country Cities: the country and its largest cities, largest first. */
export interface CountryCitiesRound extends HasCountry {
  cities: City[];
}

/**
 * The data of a round of Country Rivers: the rivers inside the country, clipped at its borders, and
 * its lakes. `major` is the more important rivers, which the game draws thicker.
 */
export interface CountryRiversRound extends HasCountry {
  major: LineGeoData;
  minor: LineGeoData;
  /** May have no polygons: a country with no large lake. */
  lakes: CountryGeoData;
  /** The name of its longest named river, or null when it has none. */
  longestRiver: string | null;
}
