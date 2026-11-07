import L from "leaflet";
import { MapContainer } from "react-leaflet";
import { LockedView } from "../../map/LockedView";
import { MapResizer } from "../../map/MapResizer";
import { MapProps } from "../../map/MapProps";
import { EndFlags, RevealTiles, RiverLine } from "./RiverLayers";
import { RiverRound } from "./model";

/** Satellite imagery with a flag at each end of the river and no line until the round ends. */
function RiverEndsMap({ data: { river }, gaveUp, gameOver }: MapProps<RiverRound>): React.JSX.Element {
  // Fit the two ends only, so the river's extent isn't given away; zoom out further for context.
  const bounds = L.latLngBounds(
    [river.source.latitude, river.source.longitude],
    [river.mouth.latitude, river.mouth.longitude],
  );

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
      {gameOver && <RiverLine river={river} gaveUp={gaveUp} />}
      <EndFlags river={river} />
      <LockedView bounds={bounds} id={river.id} locked={!gameOver} zoomOut={2} zoomIn={1} />
      <MapResizer />
    </MapContainer>
  );
}

export default RiverEndsMap;
