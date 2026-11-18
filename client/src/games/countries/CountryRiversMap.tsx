import { GameMap } from "../../map/GameMap";
import { MapProps } from "../../map/MapProps";
import { RevealTiles } from "../../map/RevealTiles";
import { AnswerOutline, FocusFit, HintLines, LakeFills, RiverLines } from "./CountryLayers";
import { CountryRiversRound } from "./model";

/**
 * The rivers inside a country, clipped at its borders, with the major ones thicker, on a blank map.
 * The first hint adds its lakes and the second its coastline. Once the round is over the outline and
 * the relief map replace them.
 */
function CountryRiversMap({
  data: { country, major, minor, lakes },
  gameOver,
  gaveUp,
  hintsUsed,
}: MapProps<CountryRiversRound>): React.JSX.Element {
  const hasLakes = lakes.geometry.coordinates.length > 0;
  const hasCoast = country.coast.geometry.coordinates.length > 0;
  return (
    <GameMap>
      <RevealTiles show={gameOver} />
      {!gameOver && hintsUsed >= 1 && hasLakes && <LakeFills data={lakes} id={`${country.name}/lakes`} />}
      {!gameOver && hintsUsed >= 2 && hasCoast && <HintLines data={country.coast} id={`${country.name}/coast`} />}
      {!gameOver && <RiverLines major={major} minor={minor} id={country.name} />}
      {gameOver && <AnswerOutline country={country} gaveUp={gaveUp} />}
      <FocusFit country={country} />
    </GameMap>
  );
}

export default CountryRiversMap;
