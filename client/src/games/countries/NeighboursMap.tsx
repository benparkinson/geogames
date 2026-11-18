import { GameMap } from "../../map/GameMap";
import { MapProps } from "../../map/MapProps";
import { RevealTiles } from "../../map/RevealTiles";
import { AnswerOutline, CountryFlag, FocusFit, HintLines, NeighbourOutlines } from "./CountryLayers";
import { CountryShapeRound } from "./model";

/** The neighbours' outlines on a blank map: the country is the gap, marked by a flag at its centre. The coastline is hint 1. */
function NeighboursMap({ data: { country, neighbours }, gameOver, gaveUp, hintsUsed }: MapProps<CountryShapeRound>): React.JSX.Element {
  return (
    <GameMap>
      <RevealTiles show={gameOver} />
      <NeighbourOutlines neighbours={neighbours} id={country.name} labelled={gameOver} />
      {!gameOver && <CountryFlag country={country} />}
      {!gameOver && hintsUsed >= 1 && <HintLines data={country.coast} id={`${country.name}/coast`} />}
      {gameOver && <AnswerOutline country={country} gaveUp={gaveUp} />}
      <FocusFit country={country} />
    </GameMap>
  );
}

export default NeighboursMap;
