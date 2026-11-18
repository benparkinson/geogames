import type { ComponentType } from "react";
import MapGame from "../common/MapGame";
import type { HintConfig } from "../common/hints";
import { useCountryClues } from "../common/useCountryClues";
import { useGuessList } from "../common/useGuessList";
import type { MapProps } from "../../map/MapProps";
import type { GameComponentProps } from "../types";
import type { CountryPairRound } from "./model";

/** What differs between the pair games; everything else is shared. */
export interface PairVariant {
  MapComponent: ComponentType<MapProps<CountryPairRound>>;
  prompt: string;
  /** Text over the map while playing, built from the round. */
  mapCaption: (round: CountryPairRound) => string;
  hints: (round: CountryPairRound) => HintConfig;
}

/** Builds the game component for one pair variant. The player names both countries. */
export function makePairGame(variant: PairVariant) {
  function PairGame({
    data,
    round,
    answerCount,
    submitAnswer,
  }: GameComponentProps<CountryPairRound>): React.JSX.Element {
    const { suggestions, isKnownGuess } = useGuessList("countries");
    const clueFor = useCountryClues();
    return (
      <MapGame
        data={data}
        answerCount={answerCount}
        MapComponent={variant.MapComponent}
        answers={data.countries.map((country) => ({ name: country.name, additionalNames: country.additionalNames }))}
        round={round}
        suggestions={suggestions}
        isKnownGuess={isKnownGuess}
        blankMap
        mapCaption={variant.mapCaption(data)}
        prompt={variant.prompt}
        inputPlaceholder="Country name…"
        answerNoun="country"
        hints={variant.hints(data)}
        clueFor={clueFor}
        submitAnswer={submitAnswer}
      />
    );
  }
  return PairGame;
}
