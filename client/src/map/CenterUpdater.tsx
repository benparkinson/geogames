import { useMap } from "react-leaflet";
import { LatLngTuple, Map } from "leaflet";

export function CenterUpdater({ center }: CenterUpdaterProps) {
  const map: Map = useMap();
  map.setView(center, 4);
  return null;
}

export class CenterUpdaterProps {
  center: LatLngTuple;
}
