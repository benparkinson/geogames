import { ReliefTiles } from "./ReliefTiles";

/** The relief map, shown once the round is over. Its tiles carry no labels. */
export function RevealTiles({ show }: { show: boolean }): React.JSX.Element | null {
  return show ? <ReliefTiles /> : null;
}
