import type { PlaceKind } from "../../api/placeNames";
import type { GameComponentProps } from "../types";
import MapGame, { type MapGameProps } from "./MapGame";
import { useCountryClues } from "./useCountryClues";
import { useGuessList } from "./useGuessList";

const PLACE_TEXT: Record<PlaceKind, { inputPlaceholder: string; answerNoun: string }> = {
  countries: { inputPlaceholder: "Country name…", answerNoun: "country" },
  rivers: { inputPlaceholder: "River name…", answerNoun: "river" },
  lakes: { inputPlaceholder: "Lake name…", answerNoun: "lake" },
};

interface PlaceGameProps<T> extends Pick<
  MapGameProps<T>,
  "MapComponent" | "answers" | "prompt" | "hints" | "blankMap" | "mapCaption"
> {
  kind: PlaceKind;
  game: GameComponentProps<T>;
}

/** A map game whose answers come from one of the place lists. */
export default function PlaceGame<T>({ kind, game, ...rest }: PlaceGameProps<T>): React.JSX.Element {
  const { suggestions, isKnownGuess } = useGuessList(kind);
  const clueFor = useCountryClues(game.round.hasClues);
  return (
    <MapGame
      {...rest}
      {...PLACE_TEXT[kind]}
      data={game.data}
      round={game.round}
      answerCount={game.answerCount}
      submitAnswer={game.submitAnswer}
      suggestions={suggestions}
      isKnownGuess={isKnownGuess}
      clueFor={clueFor}
    />
  );
}
