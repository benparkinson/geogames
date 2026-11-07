import { useEffect } from "react";
import { useMap } from "react-leaflet";
import { LatLngTuple } from "leaflet";

export function CenterUpdater({ center, zoom = 4 }: CenterUpdaterProps): null {
  const map = useMap();
  const [lat, lng] = center;

  useEffect(() => {
    map.setView([lat, lng], zoom);
  }, [map, lat, lng, zoom]);

  return null;
}

interface CenterUpdaterProps {
  center: LatLngTuple;
  zoom?: number;
}
