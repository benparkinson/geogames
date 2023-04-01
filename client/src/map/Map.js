import { MapContainer, TileLayer, Marker, GeoJSON, Tooltip } from "react-leaflet";
import L from "leaflet";
import { CenterUpdater } from "./CenterUpdater";

function Map({ tripoint, gaveUp, gameOver }) {
  let markerIcon = L.icon({
    iconUrl: "/flag.png",
    iconSize: [32, 32],
    iconAnchor: [24, 32],
  });

  const color = gaveUp ? "darkred" : "LightGoldenRodYellow";

  const borders = tripoint.countries.map((country) => (
    <GeoJSON key={country.properties.name} data={country} color={color}>
      <Tooltip>{country.properties.name}</Tooltip>
    </GeoJSON>
  ));

  return (
    <MapContainer
      center={[tripoint.coordinate.latitude, tripoint.coordinate.longitude]}
      zoom={4}
      maxZoom={6}
      minZoom={4}
    >
      <TileLayer url="https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}" />
      <Marker
        icon={markerIcon}
        position={[tripoint.coordinate.latitude, tripoint.coordinate.longitude]}
      />
      {gameOver && borders}

      <CenterUpdater center={[tripoint.coordinate.latitude, tripoint.coordinate.longitude]} />
    </MapContainer>
  );
}

export default Map;
