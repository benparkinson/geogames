import { GeoJSON } from "react-leaflet";
import { FitBounds } from "../../map/FitBounds";
import { GameMap } from "../../map/GameMap";
import { MapProps } from "../../map/MapProps";
import { RevealTiles } from "../../map/RevealTiles";
import { answerFillOptions } from "../../map/constants";
import { LakeRound } from "./model";

/** Zoom in further than for rivers: lakes are small. */
const LAKE_MAX_FIT_ZOOM = 11;

/**
 * The lake's shape on a blank map. The relief map
 * appears when the round ends. `gaveUp` is also true when the round ran out of guesses.
 */
function LakeShapesMap({ data: { lake }, gaveUp, gameOver }: MapProps<LakeRound>): React.JSX.Element {
  return (
    <GameMap maxZoom={12}>
      <RevealTiles show={gameOver} />
      <GeoJSON pathOptions={answerFillOptions(gaveUp)} data={lake.geoData} key={lake.id + (gaveUp ? "gaveUp" : "")} />
      <FitBounds data={lake.geoData} id={lake.id} maxZoom={LAKE_MAX_FIT_ZOOM} />
    </GameMap>
  );
}

export default LakeShapesMap;
