import { useEffect } from "react";
import { useMap } from "react-leaflet";
import L from "leaflet";
import type { GeoJsonObject } from "geojson";

export function FitBounds({ data, id }: { data: GeoJsonObject; id: string }): null {
  const map = useMap();

  useEffect(() => {
    map.fitBounds(L.geoJSON(data).getBounds(), { padding: [24, 24], maxZoom: 7 });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [map, id]);

  return null;
}
