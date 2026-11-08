import { TileLayer } from "react-leaflet";
import { SATELLITE_TILE_URL } from "./constants";

/** Satellite imagery, shown once the round is over. Its tiles carry no labels. */
export function RevealTiles({ show }: { show: boolean }): React.JSX.Element | null {
  return show ? <TileLayer url={SATELLITE_TILE_URL} /> : null;
}
