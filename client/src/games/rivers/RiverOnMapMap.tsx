import L from "leaflet";
import { MapContainer } from "react-leaflet";
import { LockedView } from "../../map/LockedView";
import { MapResizer } from "../../map/MapResizer";
import { MapProps } from "../../map/MapProps";
import { RevealTiles, RiverLine } from "./RiverLayers";
import { RiverRound } from "./model";

/** The river on satellite imagery from the start, with limited zoom and no panning until the round ends. */
function RiverOnMapMap({ data: { river }, gaveUp, gameOver }: MapProps<RiverRound>): React.JSX.Element {
  return (
    <MapContainer
      zoom={3}
      center={[0, 0]}
      dragging={false}
      keyboard={false}
      scrollWheelZoom="center"
      touchZoom="center"
      doubleClickZoom="center"
    >
      <RevealTiles show />
      <RiverLine river={river} gaveUp={gaveUp} />
      <LockedView bounds={L.geoJSON(river.geoData).getBounds()} id={river.id} locked={!gameOver} zoomOut={1} zoomIn={1} />
      <MapResizer />
    </MapContainer>
  );
}

export default RiverOnMapMap;
