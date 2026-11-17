import type { ComponentType } from "react";
import MapGame, { type HintConfig } from "../common/MapGame";
import { useGuessList } from "../common/useGuessList";
import type { MapProps } from "../../map/MapProps";
import type { GameComponentProps } from "../types";
import type { HasCountry } from "./model";

/** What differs between the games that ask for one country; everything else is shared. */
export interface CountryShapeVariant<T extends HasCountry> {
  MapComponent: ComponentType<MapProps<T>>;
  prompt: string;
  mapCaption: string;
  hints: (round: T) => HintConfig;
}

/**
 * Builds the game component for one variant of a game whose answer is a country: the country-shape
 * games, Country Cities and Country Rivers. The round data is anything with a `country`.
 */
export function makeCountryShapeGame<T extends HasCountry>(variant: CountryShapeVariant<T>) {
  function CountryShapeGame({ data, round, answerCount, submitAnswer }: GameComponentProps<T>): React.JSX.Element {
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
