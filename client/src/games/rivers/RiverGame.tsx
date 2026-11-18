import type { ComponentType } from "react";
import PlaceGame from "../common/PlaceGame";
import type { HintConfig } from "../common/hints";
import type { MapProps } from "../../map/MapProps";
import type { GameComponentProps } from "../types";
import type { River, RiverRound } from "./model";

/** What differs between the river games; everything else is shared. */
export interface RiverVariant {
  MapComponent: ComponentType<MapProps<RiverRound>>;
  prompt: string;
  /** Show a grey map instead of the real one while playing. */
  blankMap?: boolean;
  /** A caption over the map while playing. */
  mapCaption?: string;
  hints: (river: River) => HintConfig;
}

/** Builds the game component for one river variant. */
export function makeRiverGame(variant: RiverVariant) {
  function RiverGame(game: GameComponentProps<RiverRound>): React.JSX.Element {
    return (
      <PlaceGame
        kind="rivers"
        game={game}
        MapComponent={variant.MapComponent}
        answers={[game.data.river]}
        blankMap={variant.blankMap}
        mapCaption={variant.mapCaption}
        prompt={variant.prompt}
        hints={variant.hints(game.data.river)}
      />
    );
  }
  return RiverGame;
}
