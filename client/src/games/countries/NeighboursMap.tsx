import { MapContainer } from "react-leaflet";
import { MapResizer } from "../../map/MapResizer";
import { MapProps } from "../../map/MapProps";
import { RevealTiles } from "../../map/RevealTiles";
import { AnswerOutline, FocusFit, HintLines, NeighbourOutlines } from "./CountryLayers";
import { CountryShapeRound } from "./model";

/** The neighbours' outlines on a blank map: the country is the gap. The coastline is hint 1. */
function NeighboursMap({ data: { country, neighbours }, gameOver, gaveUp, hintsUsed }: MapProps<CountryShapeRound>): React.JSX.Element {
  return (
    <MapContainer zoom={3} minZoom={2} maxZoom={10} center={[0, 0]}>
      <RevealTiles show={gameOver} />
      <NeighbourOutlines neighbours={neighbours} id={country.name} labelled={gameOver} />
      {!gameOver && hintsUsed >= 1 && <HintLines data={country.coast} id={`${country.name}/coast`} />}
      {gameOver && <AnswerOutline country={country} gaveUp={gaveUp} />}
      <FocusFit country={country} />
      <MapResizer />
    </MapContainer>
  );
}

export default NeighboursMap;
