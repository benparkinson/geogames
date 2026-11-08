import { useEffect } from "react";
import { useMap } from "react-leaflet";
import L from "leaflet";
import type { GeoJsonObject } from "geojson";

/**
 * Fits the map to `bounds` when given, otherwise to the extent of `data`. Fits again only when
 * `id` changes.
 */
export function FitBounds({ data, bounds, id }: { data?: GeoJsonObject; bounds?: L.LatLngBoundsExpression; id: string }): null {
  const map = useMap();

  useEffect(() => {
    const target = bounds ?? (data ? L.geoJSON(data).getBounds() : undefined);
    if (target) {
      map.fitBounds(target, { padding: [24, 24], maxZoom: 7 });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [map, id]);

  return null;
}
