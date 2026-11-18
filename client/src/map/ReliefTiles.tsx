import { TileLayer } from "react-leaflet";
import { RELIEF_MAX_NATIVE_ZOOM, RELIEF_TILE_URL } from "./constants";

/**
 * The background map: Natural Earth II relief (public domain), self-hosted under `public/tiles/relief`
 * and cut by the backend's `ReliefTilesMain`. The tiles carry no labels. Natural Earth is already
 * credited on every map by `MapCredits`, so the layer has no attribution of its own.
 */
export function ReliefTiles(): React.JSX.Element {
  return <TileLayer url={RELIEF_TILE_URL} maxNativeZoom={RELIEF_MAX_NATIVE_ZOOM} />;
}
