import { MapContainer, Marker, GeoJSON, Tooltip } from "react-leaflet";
import L from "leaflet";
import { CenterUpdater } from "../../map/CenterUpdater";
import { MapResizer } from "../../map/MapResizer";
import { MapProps } from "../../map/MapProps";
import { SatelliteTiles } from "../../map/SatelliteTiles";
import { answerPathOptions } from "../../map/constants";
import { TripointRoundData } from "./model";

const markerIcon = L.icon({
  iconUrl: "/flag.png",
  iconSize: [32, 32],
  iconAnchor: [24, 32],
});

function TripointMap({ data: tripoint, gaveUp, gameOver, hintsUsed }: MapProps<TripointRoundData>) {
  const color = answerPathOptions(gaveUp);
  const position: [number, number] = [tripoint.coordinate.latitude, tripoint.coordinate.longitude];

  const answerBorders = tripoint.countries.map((country) => (
    <GeoJSON key={country.name} data={country.geoData} pathOptions={color}>
      <Tooltip sticky>{country.name}</Tooltip>
    </GeoJSON>
  ));

  const hinted = tripoint.hintOrder.slice(0, hintsUsed);
  const clueBorders = tripoint.countries
    .filter(country => hinted.includes(country.name))
    .map((country) => (
      <GeoJSON key={country.name} data={country.geoData} pathOptions={color}>
      </GeoJSON>
    ));

  return (
    <MapContainer
      center={position}
      zoom={4}
      maxZoom={10}
      minZoom={4}
    >
      <SatelliteTiles />
      <Marker icon={markerIcon} position={position} />
      {gameOver && answerBorders}
      {!gameOver && clueBorders}

      <CenterUpdater center={position} zoom={5} />
      <MapResizer />
    </MapContainer>
  );
}

export default TripointMap;
