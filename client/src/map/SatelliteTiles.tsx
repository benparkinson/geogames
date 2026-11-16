import { TileLayer } from "react-leaflet";
import { SATELLITE_ATTRIBUTION, SATELLITE_TILE_URL } from "./constants";

/** EOX Sentinel-2 cloudless (2016, CC BY 4.0), with the required credit shown in the map's attribution control. */
export function SatelliteTiles(): React.JSX.Element {
  return <TileLayer url={SATELLITE_TILE_URL} attribution={SATELLITE_ATTRIBUTION} />;
}
