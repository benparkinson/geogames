import type { BBox, GeoJsonObject, GeoJsonProperties, MultiLineString, MultiPolygon, Polygon } from "geojson";

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

/** Lines on a map (a river, a coastline, a border): a MultiLineString feature. */
export interface LineGeoData extends GeoJsonObject {
  type: "MultiLineString";
  bbox?: BBox;
  properties: GeoJsonProperties;
  geometry: MultiLineString;
}
