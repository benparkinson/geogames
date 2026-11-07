import { MapContainer } from "react-leaflet";
import { FitBounds } from "../../map/FitBounds";
import { MapResizer } from "../../map/MapResizer";
import { MapProps } from "../../map/MapProps";
import { CountryOutlines, EndFlags, RevealTiles, RiverLine } from "./RiverLayers";
import { RiverRound } from "./model";

/** The outlines of the river's countries on a blank map. The satellite map and the river come at the end. */
function RiverCountriesMap({ data: { river, countries }, gaveUp, gameOver, hintsUsed }: MapProps<RiverRound>): React.JSX.Element {
  return (
    <MapContainer zoom={3} minZoom={2} maxZoom={10} center={[0, 0]}>
      <RevealTiles show={gameOver} />
      <CountryOutlines countries={countries} />
      {gameOver && <RiverLine river={river} gaveUp={gaveUp} />}
      {(hintsUsed >= 1 || gameOver) && <EndFlags river={river} />}
      {/* Fit the river, not the countries: Russia crosses the antimeridian and would fit the whole world. */}
      <FitBounds data={river.geoData} id={river.id} />
      <MapResizer />
    </MapContainer>
  );
}

export default RiverCountriesMap;
