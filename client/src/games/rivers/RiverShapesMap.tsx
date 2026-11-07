import { MapContainer, TileLayer, GeoJSON } from "react-leaflet";
import { FitBounds } from "../../map/FitBounds";
import { MapResizer } from "../../map/MapResizer";
import { MapProps } from "../../map/MapProps";
import { answerPathOptions, SATELLITE_TILE_URL } from "../../map/constants";
import { River } from "./model";

function RiverShapesMap({ data: river, gaveUp, gameOver }: MapProps<River>): React.JSX.Element {
  const riverLine = (
    <GeoJSON pathOptions={answerPathOptions(gaveUp)} data={river.geoData} key={river.name + (gaveUp ? "gaveUp" : "")} />
  );

  return (
    <MapContainer zoom={3} maxZoom={7} minZoom={3} center={[0, 0]}>
      {gameOver && <TileLayer url={SATELLITE_TILE_URL} />}

      {riverLine}
      <FitBounds data={river.geoData} id={river.name} />
      <MapResizer />
    </MapContainer>
  );
}

export default RiverShapesMap;
