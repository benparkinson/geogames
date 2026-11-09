import { SatelliteTiles } from "./SatelliteTiles";

/** Satellite imagery, shown once the round is over. Its tiles carry no labels. */
export function RevealTiles({ show }: { show: boolean }): React.JSX.Element | null {
  return show ? <SatelliteTiles /> : null;
}
