import { useState } from "react";
import { shuffle } from "../../helper/shuffle";
import PlaceGame from "../common/PlaceGame";
import type { GameComponentProps } from "../types";
import TripointMapWrapper from "./TripointMapWrapper";
import type { TripointModel, TripointRoundData } from "./model";

export default function TripointGame(props: GameComponentProps<TripointModel>): React.JSX.Element {
  // Hints reveal country outlines in this order. It is picked once per round and never changes.
  const [data] = useState<TripointRoundData>(() => ({
    ...props.data,
    hintOrder: shuffle(props.data.countries.map((country) => country.name)),
  }));

  return (
    <PlaceGame
      kind="countries"
      game={{ ...props, data }}
      MapComponent={TripointMapWrapper}
      answers={props.data.countries}
      prompt={`Which ${props.answerCount} countries meet at this flag?`}
      mapKey="The flag marks a tripoint, the spot where their borders meet."
      hints={{ description: "Shows the outline of a country you haven't named yet" }}
    />
  );
}
