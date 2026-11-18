import { GameMap } from "../../map/GameMap";
import { MapProps } from "../../map/MapProps";
import { RevealTiles } from "../../map/RevealTiles";
import { NeighbourOutlines } from "../countries/CountryLayers";
import { CountryPairRound } from "./model";
import { AnswerOutlines, BoundsFit, HiddenBorder, MergedShape, pairId } from "./PairLayers";

/**
 * Two neighbours filled as one shape on a blank map. The first hint adds the countries around them,
 * the second the border between them.
 */
function MergedCountriesMap({ data, gameOver, gaveUp, hintsUsed }: MapProps<CountryPairRound>): React.JSX.Element {
  const id = pairId(data);
  return (
    <GameMap>
      <RevealTiles show={gameOver} />
      {(gameOver || hintsUsed >= 1) && <NeighbourOutlines neighbours={data.neighbours} id={id} labelled={gameOver} />}
      {!gameOver && <MergedShape round={data} />}
      {!gameOver && hintsUsed >= 2 && <HiddenBorder data={data.sharedBorder} id={`${id}/border`} />}
      {gameOver && <AnswerOutlines round={data} gaveUp={gaveUp} />}
      <BoundsFit bounds={data.focus} id={id} />
    </GameMap>
  );
}

export default MergedCountriesMap;
