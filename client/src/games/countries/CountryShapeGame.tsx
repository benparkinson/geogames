import type { ComponentType } from "react";
import MapGame, { type HintConfig } from "../common/MapGame";
import { useGuessList } from "../common/useGuessList";
import type { MapProps } from "../../map/MapProps";
import type { GameComponentProps } from "../types";
import type { CountryShapeRound } from "./model";

/** What differs between the country-shape games; everything else is shared. */
export interface CountryShapeVariant {
  MapComponent: ComponentType<MapProps<CountryShapeRound>>;
  prompt: string;
  mapCaption: string;
  hints: (round: CountryShapeRound) => HintConfig;
}

/** Builds the game component for one country-shape variant. */
export function makeCountryShapeGame(variant: CountryShapeVariant) {
  function CountryShapeGame({
    data,
    round,
    answerCount,
    submitAnswer,
  }: GameComponentProps<CountryShapeRound>): React.JSX.Element {
    const { suggestions, isKnownGuess } = useGuessList("countries");
    return (
      <MapGame
        data={data}
        answerCount={answerCount}
        MapComponent={variant.MapComponent}
        answers={[{ name: data.country.name, additionalNames: data.country.additionalNames }]}
        round={round}
        suggestions={suggestions}
        isKnownGuess={isKnownGuess}
        blankMap
        mapCaption={variant.mapCaption}
        prompt={variant.prompt}
        inputPlaceholder="Country name…"
        answerNoun="country"
        hints={variant.hints(data)}
        submitAnswer={submitAnswer}
      />
    );
  }
  return CountryShapeGame;
}
