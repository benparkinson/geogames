import type { BBox, GeoJsonObject, GeoJsonProperties, MultiPolygon, Polygon } from "geojson";

export interface TripointModel {
  coordinate: TripointCoordinate;
  countries: TripointCountry[];
}

/** The tripoint plus the order in which hints reveal its countries (by name). */
export interface TripointRoundData extends TripointModel {
  hintOrder: string[];
}

export interface TripointCoordinate {
  latitude: number;
  longitude: number;
}

export interface TripointCountry {
  name: string;
  additionalNames: string[];
  geoData: TripointGeoData;
}

export interface TripointGeoData extends GeoJsonObject {
  type: "MultiPolygon" | "Polygon";
  bbox?: BBox;
  properties: GeoJsonProperties;
  geometry: MultiPolygon | Polygon;
}
