import { defineGame } from "../types";
import MergedCountriesMapWrapper from "./MergedCountriesMapWrapper";
import { makePairGame } from "./PairGame";
import { hintsFrom, mapHint } from "../common/hints";
import { bothFirstLettersHint } from "./pairHints";
import { MergedCountriesIcon } from "../../ui/icons";

export default defineGame({
  type: "MERGED_COUNTRIES",
  title: "Merged Countries",
  singular: "Merged countries",
  description: "Name the two neighbouring countries merged into one shape.",
  Icon: MergedCountriesIcon,
  hasClues: true,
  Component: makePairGame({
    MapComponent: MergedCountriesMapWrapper,
    prompt: "Which two neighbouring countries make this shape?",
    // The first two hints change the map; they only say so.
    hints: (round) =>
      hintsFrom(
        "Shows the countries around them, then the hidden border, then their first letters",
        round,
        mapHint("The surrounding countries are on the map"),
        mapHint("The border between them is on the map"),
        bothFirstLettersHint,
      ),
  }),
  answers: (round) => round.countries,
});
