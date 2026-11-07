import type { BBox, GeoJsonObject, GeoJsonProperties, MultiLineString } from "geojson";

export interface River {
  name: string;
  additionalNames: string[];
  geoData: RiverGeoData;
  continent: string;
}

export interface RiverGeoData extends GeoJsonObject {
  type: "MultiLineString";
  bbox?: BBox;
  properties: GeoJsonProperties;
  geometry: MultiLineString;
}
