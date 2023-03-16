import L from "leaflet";
import icon from "leaflet/dist/images/marker-icon.png";

export function setupMap() {
  let DefaultIcon = L.icon({
    iconUrl: icon,
    iconSize: [24, 41],
    iconAnchor: [12, 41],
  });

  L.Marker.prototype.options.icon = DefaultIcon;
}
