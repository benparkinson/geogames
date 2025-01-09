import { MapContainer, TileLayer, Marker, GeoJSON, Tooltip } from "react-leaflet";
import L from "leaflet";
import { CenterUpdater } from "./CenterUpdater";
import { TripointModel } from "../games/Tripoint";

function TripointMap({ tripoint, gaveUp, gameOver }: TripointMapProps) {
  let markerIcon = L.icon({
    iconUrl: "/flag.png",
    iconSize: [32, 32],
    iconAnchor: [24, 32],
  });

  const color = gaveUp ? { color: "darkred" } : { color: "LightGoldenRodYellow" };

  const answerBorders = tripoint.countries.map((country) => (
    <GeoJSON key={country.name} data={country.geoData} pathOptions={color}>
      <Tooltip>{country.name}</Tooltip>
    </GeoJSON>
  ));

  const clueBorders = tripoint.countries
    .filter(country => country.showAsHint)
    .map((country) => (
      <GeoJSON key={country.name} data={country.geoData} pathOptions={color}>
      </GeoJSON>
    ));

  return (
    <MapContainer
      center={[tripoint.coordinate.latitude, tripoint.coordinate.longitude]}
      zoom={4}
      maxZoom={10}
      minZoom={4}
    >
      <TileLayer url="https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}" />
      <Marker
        icon={markerIcon}
        position={[tripoint.coordinate.latitude, tripoint.coordinate.longitude]}
      />
      {gameOver && answerBorders}
      {!gameOver && clueBorders}

      <CenterUpdater center={[tripoint.coordinate.latitude, tripoint.coordinate.longitude]} />
    </MapContainer>
  );
}

export class TripointMapProps {
  tripoint: TripointModel;
  gaveUp: boolean;
  gameOver: boolean;
}

export default TripointMap;
