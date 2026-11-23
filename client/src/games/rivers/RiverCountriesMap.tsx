import { boundsToLatLng } from "../../map/boundsToLatLng";
import { FitBounds } from "../../map/FitBounds";
import { RevealTiles } from "../../map/RevealTiles";
import { GameMap } from "../../map/GameMap";
import { MapProps } from "../../map/MapProps";
import { CountryOutlines, EndPins, RiverLine } from "./RiverLayers";
import { RiverRound } from "./model";

/** The outlines of the river's countries on a blank map. The relief map and the river come at the end. */
function RiverCountriesMap({
  data: { river, countries, focus },
  gaveUp,
  gameOver,
  hintsUsed,
}: MapProps<RiverRound>): React.JSX.Element {
  return (
    <GameMap>
      <RevealTiles show={gameOver} />
      <CountryOutlines countries={countries} labelled={gameOver} />
      {gameOver && <RiverLine river={river} gaveUp={gaveUp} />}
      {(hintsUsed >= 1 || gameOver) && <EndPins river={river} />}
      {/* The server's box already leaves out overseas parts and caps a huge country, so every outline
          shows at the start. A response cached before the server sent it fits the river instead. */}
      {focus ? (
        <FitBounds id={river.id} bounds={boundsToLatLng(focus)} />
      ) : (
        <FitBounds data={river.geoData} id={river.id} />
      )}
    </GameMap>
  );
}

export default RiverCountriesMap;
