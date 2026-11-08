import { useState } from "react";
import { shuffle } from "../../helper/shuffle";
import MapGame from "../common/MapGame";
import { useGuessList } from "../common/useGuessList";
import type { GameComponentProps } from "../types";
import TripointMapWrapper from "./TripointMapWrapper";
import type { TripointModel, TripointRoundData } from "./model";

export default function TripointGame({ data: tripoint, round, answerCount, submitAnswer }: GameComponentProps<TripointModel>): React.JSX.Element {
  // Hints reveal country outlines in this order. It is picked once per round and never changes.
  const [data] = useState<TripointRoundData>(() => ({
    ...tripoint,
    hintOrder: shuffle(tripoint.countries.map(country => country.name)),
  }));
  const { suggestions, isKnownGuess } = useGuessList("countries");

  return (
    <MapGame
      data={data}
      answerCount={answerCount}
      MapComponent={TripointMapWrapper}
      answers={tripoint.countries}
      round={round}
      suggestions={suggestions}
      isKnownGuess={isKnownGuess}
      prompt={`Name the ${answerCount} countries that meet at the flag.`}
      inputPlaceholder="Country name…"
      answerNoun="country"
      hints={{
        description: "Shows the outline of one of the countries",
      }}
      submitAnswer={submitAnswer}
    />
  );
}
