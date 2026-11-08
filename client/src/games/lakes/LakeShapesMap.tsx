import { GeoJSON, MapContainer, ScaleControl } from "react-leaflet";
import { FitBounds } from "../../map/FitBounds";
import { MapProps } from "../../map/MapProps";
import { MapResizer } from "../../map/MapResizer";
import { RevealTiles } from "../../map/RevealTiles";
import { answerPathOptions } from "../../map/constants";
import { LakeRound } from "./model";

/** Zoom in further than for rivers: lakes are small. */
const LAKE_MAX_FIT_ZOOM = 11;

/**
 * The lake's shape on a blank map, with a scale bar because size is a fair clue. The satellite map
 * appears when the round ends. `gaveUp` is also true when the round ran out of guesses.
 */
function LakeShapesMap({ data: { lake }, gaveUp, gameOver }: MapProps<LakeRound>): React.JSX.Element {
  return (
    <MapContainer zoom={3} minZoom={2} maxZoom={12} center={[0, 0]}>
      <RevealTiles show={gameOver} />
      <GeoJSON pathOptions={{ ...answerPathOptions(gaveUp), fillOpacity: 0.35 }} data={lake.geoData} key={lake.id + (gaveUp ? "gaveUp" : "")} />
      <FitBounds data={lake.geoData} id={lake.id} maxZoom={LAKE_MAX_FIT_ZOOM} />
      <ScaleControl position="bottomleft" imperial={false} />
      <MapResizer />
    </MapContainer>
  );
}

export default LakeShapesMap;
