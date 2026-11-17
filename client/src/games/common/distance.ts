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
  const h =
    Math.sin(dLat / 2) ** 2 + Math.cos(toRadians(a.lat)) * Math.cos(toRadians(b.lat)) * Math.sin(dLon / 2) ** 2;
  return 2 * EARTH_RADIUS_KM * Math.asin(Math.min(1, Math.sqrt(h)));
}

/** The initial bearing from one position to another, in degrees from north: 0 up to (not including) 360. */
export function bearingDegrees(from: LatLon, to: LatLon): number {
  const dLon = toRadians(to.lon - from.lon);
  const lat1 = toRadians(from.lat);
  const lat2 = toRadians(to.lat);
  const y = Math.sin(dLon) * Math.cos(lat2);
  const x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);
  return ((Math.atan2(y, x) * 180) / Math.PI + 360) % 360;
}

/** One of 8 compass points: sectors of 45° centred on each point. */
export function compassPoint(bearing: number): CompassPoint {
  const sector = Math.floor((((bearing % 360) + 360) % 360 + 22.5) / 45) % 8;
  return COMPASS_POINTS[sector];
}

/** A distance rounded to the nearest 10 km, with thousands separators: `1,240`. */
export function formatKm(km: number): string {
  return (Math.round(km / 10) * 10).toLocaleString("en-GB");
}
