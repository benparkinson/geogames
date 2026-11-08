import { MapContainer } from "react-leaflet";
import { FitBounds } from "../../map/FitBounds";
import { RevealTiles } from "../../map/RevealTiles";
import { MapResizer } from "../../map/MapResizer";
import { MapProps } from "../../map/MapProps";
import { RiverLine } from "./RiverLayers";
import { RiverRound } from "./model";

function RiverShapesMap({ data: { river }, gaveUp, gameOver }: MapProps<RiverRound>): React.JSX.Element {
  return (
    <MapContainer zoom={3} maxZoom={7} minZoom={3} center={[0, 0]}>
      <RevealTiles show={gameOver} />
      <RiverLine river={river} gaveUp={gaveUp} />
      <FitBounds data={river.geoData} id={river.id} />
      <MapResizer />
    </MapContainer>
  );
}

export default RiverShapesMap;
