import { MapContainer, TileLayer, GeoJSON } from "react-leaflet";
import { CenterUpdater } from "./CenterUpdater";

function RiverShapesMap({ river, gaveUp, gameOver }) {
  function arrayMid(array) {
    return array[Math.round((array.length - 1) / 2)];
  }

  const color = gaveUp ? "darkred" : "LightGoldenRodYellow";

  const middle = arrayMid(river.geometry.coordinates);
  const mapCenter = arrayMid(middle).slice().reverse();

  return (
    <MapContainer zoom={7} maxZoom={7} minZoom={3} center={mapCenter}>
      {gameOver && (
        <TileLayer url="https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}" />
      )}

      <GeoJSON color={color} data={river} key={river.properties.name} />
      <CenterUpdater center={mapCenter} />
    </MapContainer>
  );
}

export default RiverShapesMap;
