import { GameMap } from "../../map/GameMap";
import { MapProps } from "../../map/MapProps";
import { RevealTiles } from "../../map/RevealTiles";
import { AnswerOutline, ClueLines } from "./CountryLayers";
import { CapitalDots, CitiesFit, CityDots } from "./CityLayers";
import { CountryCitiesRound } from "./model";

/**
 * The country's cities as dots sized by population on a blank map, with no outline. The first hint
 * highlights the capital and the second adds the coastline. Once the round is over the outline and the
 * relief map appear, and each dot shows its city's name (the outline itself is not labelled).
 * The map starts framed on the country's main part and every city, so none is off screen.
 */
function CountryCitiesMap({
  data: { country, cities },
  gameOver,
  gaveUp,
  hintsUsed,
}: MapProps<CountryCitiesRound>): React.JSX.Element {
  const hasCoast = country.coast.geometry.coordinates.length > 0;
  return (
    <GameMap>
      <RevealTiles show={gameOver} />
      {gameOver && <AnswerOutline country={country} gaveUp={gaveUp} labelled={false} />}
      {!gameOver && hintsUsed >= 2 && hasCoast && <ClueLines data={country.coast} id={`${country.name}/coast`} />}
      <CityDots cities={cities} labelled={gameOver} />
      {(gameOver || hintsUsed >= 1) && <CapitalDots cities={cities} labelled={gameOver} />}
      <CitiesFit country={country} cities={cities} />
    </GameMap>
  );
}

export default CountryCitiesMap;
