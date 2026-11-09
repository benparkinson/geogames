import { TileLayer } from "react-leaflet";
import { SATELLITE_ATTRIBUTION, SATELLITE_TILE_URL } from "./constants";

/** Esri World Imagery, with the credit Esri's terms require shown in the map's attribution control. */
export function SatelliteTiles(): React.JSX.Element {
  return <TileLayer url={SATELLITE_TILE_URL} attribution={SATELLITE_ATTRIBUTION} />;
}
