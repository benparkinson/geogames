/** A bounding box as the corner pair Leaflet's `fitBounds` takes: south-west, then north-east. */
export function boundsToLatLng({
  south,
  west,
  north,
  east,
}: {
  south: number;
  west: number;
  north: number;
  east: number;
}): [[number, number], [number, number]] {
  return [
    [south, west],
    [north, east],
  ];
}
