import { useEffect } from "react";
import { useMap } from "react-leaflet";
import L from "leaflet";

/** Zoom range once the view is unlocked, so the player can explore the revealed map. */
const UNLOCKED_MIN_ZOOM = 2;
const UNLOCKED_MAX_ZOOM = 10;

/**
 * Fits the map to `bounds`, limits zoom to a few levels either side and (while `locked`) turns off
 * panning. When `locked` becomes false the zoom range opens up and panning comes back.
 *
 * The map must also set `scrollWheelZoom`, `touchZoom` and `doubleClickZoom` to "center", or
 * zooming at the cursor pans the map.
 */
export function LockedView({
  bounds,
  id,
  locked,
  zoomOut = 1,
  zoomIn = 1,
}: {
  bounds: L.LatLngBounds;
  id: string;
  locked: boolean;
  zoomOut?: number;
  zoomIn?: number;
}): null {
  const map = useMap();

  useEffect(() => {
    const fit = map.getBoundsZoom(bounds, false, L.point(48, 48));
    // Order matters. Narrowing the limits while the current zoom is outside them makes Leaflet
    // start an animated zoom around the *current* centre, and Leaflet ignores any setView made
    // while that animation runs, which strands the map at its initial [0, 0]. So widen the limits
    // (which never moves the map), jump to the river without animating, then narrow the limits
    // around a zoom that is already inside them.
    map.setMinZoom(Math.min(fit - zoomOut, UNLOCKED_MIN_ZOOM));
    map.setMaxZoom(Math.max(fit + zoomIn, UNLOCKED_MAX_ZOOM));
    map.setView(bounds.getCenter(), fit, { animate: false });
    map.setMinZoom(fit - zoomOut);
    map.setMaxZoom(fit + zoomIn);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [map, id]);

  useEffect(() => {
    if (locked) {
      map.dragging.disable();
      map.keyboard.disable();
      map.boxZoom.disable();
      return;
    }
    map.dragging.enable();
    map.keyboard.enable();
    map.boxZoom.enable();
    map.setMinZoom(UNLOCKED_MIN_ZOOM);
    map.setMaxZoom(UNLOCKED_MAX_ZOOM);
  }, [map, locked]);

  return null;
}
