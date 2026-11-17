import { MapContainer } from "react-leaflet";
import { MapResizer } from "../../map/MapResizer";
import { MapProps } from "../../map/MapProps";
import { RevealTiles } from "../../map/RevealTiles";
import { AnswerOutline, FocusFit, HintLines } from "./CountryLayers";
import { CapitalDots, CityDots } from "./CityLayers";
import { CountryCitiesRound } from "./model";

/**
 * The country's cities as dots sized by population on a blank map, with no outline. The first hint
 * highlights the capital and the second adds the coastline. Once the round is over the outline and the
 * satellite imagery appear, and each dot shows its city's name.
 */
function CountryCitiesMap({ data: { country, cities }, gameOver, gaveUp, hintsUsed }: MapProps<CountryCitiesRound>): React.JSX.Element {
  const hasCoast = country.coast.geometry.coordinates.length > 0;
  return (
    <MapContainer zoom={3} minZoom={2} maxZoom={10} center={[0, 0]}>
      <RevealTiles show={gameOver} />
      {gameOver && <AnswerOutline country={country} gaveUp={gaveUp} />}
      {!gameOver && hintsUsed >= 2 && hasCoast && <HintLines data={country.coast} id={`${country.name}/coast`} />}
      <CityDots cities={cities} labelled={gameOver} />
      {(gameOver || hintsUsed >= 1) && <CapitalDots cities={cities} labelled={gameOver} />}
      <FocusFit country={country} />
      <MapResizer />
    </MapContainer>
  );
}

export default CountryCitiesMap;
