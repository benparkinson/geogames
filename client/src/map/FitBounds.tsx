import { useEffect } from "react";
import { useMap } from "react-leaflet";
import L from "leaflet";
import type { GeoJsonObject } from "geojson";

/**
 * Fits the map to `bounds` when given, otherwise to the extent of `data`, without zooming in past
 * `maxZoom` (7 unless given). Fits again only when `id` changes.
 */
export function FitBounds({ data, bounds, id, maxZoom = 7 }: { data?: GeoJsonObject; bounds?: L.LatLngBoundsExpression; id: string; maxZoom?: number }): null {
  const map = useMap();

  useEffect(() => {
    const target = bounds ?? (data ? L.geoJSON(data).getBounds() : undefined);
    if (target) {
      map.fitBounds(target, { padding: [24, 24], maxZoom });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [map, id]);

  return null;
}
