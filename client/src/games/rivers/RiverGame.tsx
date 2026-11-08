import type { ComponentType } from "react";
import MapGame, { type HintConfig } from "../common/MapGame";
import { useGuessList } from "../common/useGuessList";
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
  function RiverGame({ data, round, answerCount, submitAnswer }: GameComponentProps<RiverRound>): React.JSX.Element {
    const { suggestions, isKnownGuess } = useGuessList("rivers");
    return (
      <MapGame
        data={data}
        answerCount={answerCount}
        MapComponent={variant.MapComponent}
        answers={[data.river]}
        round={round}
        suggestions={suggestions}
        isKnownGuess={isKnownGuess}
        blankMap={variant.blankMap}
        mapCaption={variant.mapCaption}
        prompt={variant.prompt}
        inputPlaceholder="River name…"
        answerNoun="river"
        hints={variant.hints(data.river)}
        submitAnswer={submitAnswer}
      />
    );
  }
  return RiverGame;
}
