import { useMemo, useState } from "react";
import { useCountryNames } from "../../api/countries";
import { normaliseString } from "../../helper/stringHelper";
import { shuffle } from "../../helper/shuffle";
import MapGame from "../common/MapGame";
import type { GameComponentProps } from "../types";
import TripointMapWrapper from "./TripointMapWrapper";
import type { TripointModel, TripointRoundData } from "./model";

export default function TripointGame({ data: tripoint, round, answerCount, submitAnswer }: GameComponentProps<TripointModel>): React.JSX.Element {
  // Hints reveal country outlines in this order. It is picked once per round and never changes.
  const [data] = useState<TripointRoundData>(() => ({
    ...tripoint,
    hintOrder: shuffle(tripoint.countries.map(country => country.name)),
  }));
  const { data: countryList } = useCountryNames();

  const suggestions = useMemo(() => countryList?.map(c => c.name), [countryList]);
  const knownNames = useMemo(() => {
    if (!countryList) {
      return null;
    }
    const names = new Set<string>();
    countryList.forEach(c => {
      names.add(normaliseString(c.name));
      c.additionalNames.forEach(alias => names.add(normaliseString(alias)));
    });
    return names;
  }, [countryList]);

  return (
    <MapGame
      data={data}
      answerCount={answerCount}
      MapComponent={TripointMapWrapper}
      answers={tripoint.countries}
      round={round}
      suggestions={suggestions}
      isKnownGuess={knownNames ? (guess: string) => knownNames.has(normaliseString(guess)) : undefined}
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
