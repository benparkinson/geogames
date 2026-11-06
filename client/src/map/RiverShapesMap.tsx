import { MapContainer, TileLayer, GeoJSON } from "react-leaflet";
import { FitBounds } from "./FitBounds";
import { MapResizer } from "./MapResizer";
import { River } from "../games/RiverShapes";

function RiverShapesMap({ river, gaveUp, gameOver }: RiverShapesMapProps): JSX.Element {
  const riverLine = gaveUp ? (
    <GeoJSON pathOptions={{ color: "darkred" }} data={river.geoData} key={river.name + "gaveUp"} />
  ) : (
    <GeoJSON pathOptions={{ color: "LightGoldenRodYellow" }} data={river.geoData} key={river.name} />
  );

  return (
    <MapContainer zoom={3} maxZoom={7} minZoom={3} center={[0, 0]}>
      {gameOver && (
        <TileLayer url="https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}" />
      )}

      {riverLine}
      <FitBounds data={river.geoData} id={river.name} />
      <MapResizer />
    </MapContainer>
  );
}

export class RiverShapesMapProps {
  river: River;
  gaveUp: boolean;
  gameOver: boolean;
}

export default RiverShapesMap;
