import { GameMap } from "../../map/GameMap";
import { MapProps } from "../../map/MapProps";
import { RevealTiles } from "../../map/RevealTiles";
import { AnswerOutline, ClueLines, FocusFit, HintLines, NeighbourOutlines } from "./CountryLayers";
import { CountryShapeRound } from "./model";

/** Only the land borders, as lines on a blank map. The coastline is hint 1 and the neighbours hint 2. */
function LandBordersMap({
  data: { country, neighbours },
  gameOver,
  gaveUp,
  hintsUsed,
}: MapProps<CountryShapeRound>): React.JSX.Element {
  return (
    <GameMap>
      <RevealTiles show={gameOver} />
      {(gameOver || hintsUsed >= 2) && (
        <NeighbourOutlines neighbours={neighbours} id={country.name} labelled={gameOver} />
      )}
      {!gameOver && <ClueLines data={country.landBorders} id={`${country.name}/borders`} />}
      {!gameOver && hintsUsed >= 1 && <HintLines data={country.coast} id={`${country.name}/coast`} />}
      {gameOver && <AnswerOutline country={country} gaveUp={gaveUp} />}
      <FocusFit country={country} />
    </GameMap>
  );
}

export default LandBordersMap;
