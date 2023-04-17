import { MapContainer, TileLayer, GeoJSON } from "react-leaflet";
import type { Position } from "geojson";
import { CenterUpdater } from "./CenterUpdater";
import { River } from "../games/RiverShapes";
import { LatLngTuple } from "leaflet";

function RiverShapesMap({ river, gaveUp, gameOver }: RiverShapesMapProps): JSX.Element {

  function arrayMid<Type>(array: Type[]): Type {
    return array[Math.round((array.length - 1) / 2)];
  }

  const riverLine = gaveUp ? (
    <GeoJSON pathOptions={{ color: "darkred" }} data={river.geoData} key={river.name + "gaveUp"} />
  ) : (
    <GeoJSON pathOptions={{ color: "LightGoldenRodYellow" }} data={river.geoData} key={river.name} />
  );

  const middle: Position[] = arrayMid(river.geoData.geometry.coordinates);
  const mapCenter: Position = arrayMid(middle).slice().reverse();

  return (
    <MapContainer zoom={7} maxZoom={7} minZoom={3} center={mapCenter as LatLngTuple}>
      {gameOver && (
        <TileLayer url="https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}" />
      )}

      {riverLine}
      <CenterUpdater center={mapCenter as LatLngTuple} />
    </MapContainer>
  );
}

export class RiverShapesMapProps {
  river: River;
  gaveUp: boolean;
  gameOver: boolean;
}

export default RiverShapesMap;
