export interface LatLon {
  lat: number;
  lon: number;
}

const EARTH_RADIUS_KM = 6371;

const COMPASS_POINTS = [
  "north",
  "north-east",
  "east",
  "south-east",
  "south",
  "south-west",
  "west",
  "north-west",
] as const;

export type CompassPoint = (typeof COMPASS_POINTS)[number];

const toRadians = (degrees: number): number => (degrees * Math.PI) / 180;

/** The great-circle (haversine) distance between two positions, in km. */
export function distanceKm(a: LatLon, b: LatLon): number {
  const dLat = toRadians(b.lat - a.lat);
  const dLon = toRadians(b.lon - a.lon);
  const h = Math.sin(dLat / 2) ** 2 + Math.cos(toRadians(a.lat)) * Math.cos(toRadians(b.lat)) * Math.sin(dLon / 2) ** 2;
  return 2 * EARTH_RADIUS_KM * Math.asin(Math.min(1, Math.sqrt(h)));
}

/** The Mercator "stretched" latitude, so that a constant bearing is a straight line on a flat map. */
const mercatorY = (lat: number): number =>
  Math.log(Math.tan(Math.PI / 4 + toRadians(Math.min(89.9, Math.max(-89.9, lat))) / 2));

/**
 * The direction from one position to another as it looks on a flat map, in degrees from north: 0 up to (not including)
 * 360. It is the constant (rhumb line) bearing, taking the shorter way round in longitude, not the great-circle initial
 * bearing: that one bends over the poles for far-apart places (South Korea to Paraguay starts "north-east"), which
 * doesn't match the map the player is looking at.
 */
export function bearingDegrees(from: LatLon, to: LatLon): number {
  const dLon = ((((to.lon - from.lon) % 360) + 540) % 360) - 180;
  const dY = mercatorY(to.lat) - mercatorY(from.lat);
  return ((Math.atan2(toRadians(dLon), dY) * 180) / Math.PI + 360) % 360;
}

/** One of 8 compass points: sectors of 45° centred on each point. */
export function compassPoint(bearing: number): CompassPoint {
  const sector = Math.floor(((((bearing % 360) + 360) % 360) + 22.5) / 45) % 8;
  return COMPASS_POINTS[sector];
}

/** A distance rounded to the nearest 10 km, with thousands separators: `1,240`. */
export function formatKm(km: number): string {
  return (Math.round(km / 10) * 10).toLocaleString("en-GB");
}
