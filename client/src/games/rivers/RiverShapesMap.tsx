import { FitBounds } from "../../map/FitBounds";
import { RevealTiles } from "../../map/RevealTiles";
import { GameMap } from "../../map/GameMap";
import { MapProps } from "../../map/MapProps";
import { RiverLine } from "./RiverLayers";
import { RiverRound } from "./model";

function RiverShapesMap({ data: { river }, gaveUp, gameOver }: MapProps<RiverRound>): React.JSX.Element {
  return (
    <GameMap minZoom={3} maxZoom={7}>
      <RevealTiles show={gameOver} />
      <RiverLine river={river} gaveUp={gaveUp} />
      <FitBounds data={river.geoData} id={river.id} />
    </GameMap>
  );
}

export default RiverShapesMap;
