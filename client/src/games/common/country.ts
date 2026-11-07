import type { BBox, GeoJsonObject, GeoJsonProperties, MultiPolygon, Polygon } from "geojson";

/** A country as the server sends it: its names and outline. */
export interface Country {
  name: string;
  additionalNames: string[];
  geoData: CountryGeoData;
}

export interface CountryGeoData extends GeoJsonObject {
  type: "MultiPolygon" | "Polygon";
  bbox?: BBox;
  properties: GeoJsonProperties;
  geometry: MultiPolygon | Polygon;
}

export interface Coordinate {
  latitude: number;
  longitude: number;
}
