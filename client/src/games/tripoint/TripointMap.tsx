import { Marker, GeoJSON, Tooltip } from "react-leaflet";
import L from "leaflet";
import { CenterUpdater } from "../../map/CenterUpdater";
import { GameMap } from "../../map/GameMap";
import { MapProps } from "../../map/MapProps";
import { ReliefTiles } from "../../map/ReliefTiles";
import { answerPathOptions } from "../../map/constants";
import { hintedCountries, TripointRoundData } from "./model";

const markerIcon = L.icon({
  iconUrl: "/flag.png",
  iconSize: [32, 32],
  iconAnchor: [24, 32],
});

function TripointMap({ data: tripoint, gaveUp, gameOver, hintsUsed, foundAtHint }: MapProps<TripointRoundData>) {
  const color = answerPathOptions(gaveUp);
  const position: [number, number] = [tripoint.coordinate.latitude, tripoint.coordinate.longitude];

  const answerBorders = tripoint.countries.map((country) => (
    <GeoJSON key={country.name} data={country.geoData} pathOptions={color}>
      <Tooltip sticky>{country.name}</Tooltip>
    </GeoJSON>
  ));

  const hinted = hintedCountries(tripoint.hintOrder, hintsUsed, foundAtHint);
  const clueBorders = tripoint.countries
    .filter((country) => hinted.includes(country.name))
    .map((country) => <GeoJSON key={country.name} data={country.geoData} pathOptions={color}></GeoJSON>);

  return (
    <GameMap center={position} zoom={4} minZoom={4} credit="Tripoints: Wikipedia (CC BY-SA 4.0), positions adjusted">
      <ReliefTiles />
      <Marker icon={markerIcon} position={position} />
      {gameOver && answerBorders}
      {!gameOver && clueBorders}

      <CenterUpdater center={position} zoom={5} />
    </GameMap>
  );
}

export default TripointMap;
