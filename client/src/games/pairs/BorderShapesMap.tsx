import { GameMap } from "../../map/GameMap";
import { MapProps } from "../../map/MapProps";
import { RevealTiles } from "../../map/RevealTiles";
import { ClueLines, HintLines } from "../countries/CountryLayers";
import { CountryPairRound } from "./model";
import { AnswerOutlines, BoundsFit, pairId } from "./PairLayers";

/**
 * The border between two countries as a lone line on a blank map, framed tightly. The first hint adds
 * both countries' other land borders and widens the frame to both countries, the second both coastlines.
 */
function BorderShapesMap({ data, gameOver, gaveUp, hintsUsed }: MapProps<CountryPairRound>): React.JSX.Element {
  const id = pairId(data);
  const wide = gameOver || hintsUsed >= 1;
  return (
    <GameMap>
      <RevealTiles show={gameOver} />
      {!gameOver && hintsUsed >= 1 && data.countries.map((country) => <HintLines key={`${country.name}/borders`} data={country.landBorders} id={`${id}/${country.name}/borders`} />)}
      {!gameOver && hintsUsed >= 2 && data.countries.map((country) => <HintLines key={`${country.name}/coast`} data={country.coast} id={`${id}/${country.name}/coast`} />)}
      {!gameOver && <ClueLines data={data.sharedBorder} id={`${id}/border`} />}
      {gameOver && <AnswerOutlines round={data} gaveUp={gaveUp} />}
      <BoundsFit bounds={wide ? data.focus : data.borderBounds} id={`${id}/${wide ? "focus" : "border"}`} />
    </GameMap>
  );
}

export default BorderShapesMap;
