import { GeoJSON, TileLayer } from "react-leaflet";
import { answerPathOptions, SATELLITE_TILE_URL } from "../../map/constants";
import type { River } from "./model";

/** The river's line. Re-created when the player gives up so the colour changes. */
export function RiverLine({ river, gaveUp }: { river: River; gaveUp: boolean }): React.JSX.Element {
  return <GeoJSON pathOptions={answerPathOptions(gaveUp)} data={river.geoData} key={river.id + (gaveUp ? "gaveUp" : "")} />;
}

/** Satellite imagery, shown once the round is over. Its tiles carry no labels. */
export function RevealTiles({ show }: { show: boolean }): React.JSX.Element | null {
  return show ? <TileLayer url={SATELLITE_TILE_URL} /> : null;
}
