import { GameMap } from "../../map/GameMap";
import { MapProps } from "../../map/MapProps";
import { RevealTiles } from "../../map/RevealTiles";
import { AnswerOutline, ClueLines, FocusFit, HintLines } from "./CountryLayers";
import { CountryShapeRound } from "./model";

/** Only the coastline, as lines on a blank map. The land borders are hint 2. */
function CoastlineMap({ data: { country }, gameOver, gaveUp, hintsUsed }: MapProps<CountryShapeRound>): React.JSX.Element {
  return (
    <GameMap>
      <RevealTiles show={gameOver} />
      {!gameOver && <ClueLines data={country.coast} id={`${country.name}/coast`} />}
      {!gameOver && hintsUsed >= 2 && <HintLines data={country.landBorders} id={`${country.name}/borders`} />}
      {gameOver && <AnswerOutline country={country} gaveUp={gaveUp} />}
      <FocusFit country={country} />
    </GameMap>
  );
}

export default CoastlineMap;
