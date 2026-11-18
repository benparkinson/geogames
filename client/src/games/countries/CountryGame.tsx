import type { ComponentType } from "react";
import PlaceGame from "../common/PlaceGame";
import type { HintConfig } from "../common/hints";
import type { MapProps } from "../../map/MapProps";
import type { GameComponentProps } from "../types";
import type { HasCountry } from "./model";

/** What differs between the games that ask for one country; everything else is shared. */
export interface CountryVariant<T extends HasCountry> {
  MapComponent: ComponentType<MapProps<T>>;
  prompt: string;
  mapCaption: string;
  hints: (round: T) => HintConfig;
}

/**
 * Builds the game component for one variant of a game whose answer is a country: the country-shape
 * games, Country Cities and Country Rivers. The round data is anything with a `country`.
 */
export function makeCountryGame<T extends HasCountry>(variant: CountryVariant<T>) {
  function CountryGame(game: GameComponentProps<T>): React.JSX.Element {
    const { country } = game.data;
    return (
      <PlaceGame
        kind="countries"
        game={game}
        MapComponent={variant.MapComponent}
        answers={[{ name: country.name, additionalNames: country.additionalNames }]}
        blankMap
        mapCaption={variant.mapCaption}
        prompt={variant.prompt}
        hints={variant.hints(game.data)}
      />
    );
  }
  return CountryGame;
}
