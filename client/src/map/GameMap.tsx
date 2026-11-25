import type { ReactNode } from "react";
import { MapContainer, type MapContainerProps } from "react-leaflet";
import { MapCredits } from "./MapCredits";
import { MapResizer } from "./MapResizer";
import { MapScale } from "./MapScale";

interface GameMapProps extends MapContainerProps {
  children: ReactNode;
  /** A credit only this map needs, shown before the standard one. */
  credit?: string;
}

/** The container for every game map, so each one has its credits, a scale bar, and resizes with the page. */
export function GameMap({
  children,
  credit,
  zoom = 3,
  minZoom = 2,
  maxZoom = 10,
  center = [0, 0],
  ...rest
}: GameMapProps): React.JSX.Element {
  return (
    <MapContainer zoom={zoom} minZoom={minZoom} maxZoom={maxZoom} center={center} {...rest}>
      {children}
      <MapCredits extra={credit} />
      <MapScale />
      <MapResizer />
    </MapContainer>
  );
}
