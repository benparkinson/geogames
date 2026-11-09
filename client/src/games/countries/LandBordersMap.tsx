import { MapContainer } from "react-leaflet";
import { MapResizer } from "../../map/MapResizer";
import { MapProps } from "../../map/MapProps";
import { RevealTiles } from "../../map/RevealTiles";
import { AnswerOutline, ClueLines, FocusFit, HintLines, NeighbourOutlines } from "./CountryLayers";
import { CountryShapeRound } from "./model";

/** Only the land borders, as lines on a blank map. The neighbours are hint 1 and the coastline hint 2. */
function LandBordersMap({ data: { country, neighbours }, gameOver, gaveUp, hintsUsed }: MapProps<CountryShapeRound>): React.JSX.Element {
  return (
    <MapContainer zoom={3} minZoom={2} maxZoom={10} center={[0, 0]}>
      <RevealTiles show={gameOver} />
      {(gameOver || hintsUsed >= 1) && <NeighbourOutlines neighbours={neighbours} id={country.name} labelled={gameOver} />}
      {!gameOver && <ClueLines data={country.landBorders} id={`${country.name}/borders`} />}
      {!gameOver && hintsUsed >= 2 && <HintLines data={country.coast} id={`${country.name}/coast`} />}
      {gameOver && <AnswerOutline country={country} gaveUp={gaveUp} />}
      <FocusFit country={country} />
      <MapResizer />
    </MapContainer>
  );
}

export default LandBordersMap;
