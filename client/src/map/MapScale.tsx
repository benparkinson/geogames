import { ScaleControl } from "react-leaflet";
import { useDistanceUnit } from "../settings/units";

export const SCALE_MAX_WIDTH = 80;

/**
 * A scale bar in the player's distance unit. It remounts on a change because Leaflet reads the options once.
 */
export function MapScale(): React.JSX.Element {
  const [unit] = useDistanceUnit();
  return (
    <ScaleControl
      key={unit}
      position="bottomleft"
      maxWidth={SCALE_MAX_WIDTH}
      metric={unit === "km"}
      imperial={unit === "mi"}
    />
  );
}
