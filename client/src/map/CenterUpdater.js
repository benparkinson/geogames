import { useMap } from "react-leaflet";

export function CenterUpdater({ center }) {
  const map = useMap();
  map.setView(center, 4);
  return null;
}
