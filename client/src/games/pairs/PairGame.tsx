import type { ComponentType } from "react";
import PlaceGame from "../common/PlaceGame";
import type { HintConfig } from "../common/hints";
import type { MapProps } from "../../map/MapProps";
import type { GameComponentProps } from "../types";
import type { CountryPairRound } from "./model";

/** What differs between the pair games; everything else is shared. */
export interface PairVariant {
  MapComponent: ComponentType<MapProps<CountryPairRound>>;
  prompt: string;
  /** What the marks on the map are, built from the round. */
  hints: (round: CountryPairRound) => HintConfig;
}

/** Builds the game component for one pair variant. The player names both countries. */
export function makePairGame(variant: PairVariant) {
  function PairGame(game: GameComponentProps<CountryPairRound>): React.JSX.Element {
    return (
      <PlaceGame
        kind="countries"
        game={game}
        MapComponent={variant.MapComponent}
        answers={game.data.countries.map((country) => ({
          name: country.name,
          additionalNames: country.additionalNames,
        }))}
        blankMap
        prompt={variant.prompt}
        hints={variant.hints(game.data)}
      />
    );
  }
  return PairGame;
}
