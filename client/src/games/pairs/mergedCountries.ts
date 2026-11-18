import { Union } from "react-bootstrap-icons";
import { defineGame } from "../types";
import MergedCountriesMapWrapper from "./MergedCountriesMapWrapper";
import { makePairGame } from "./PairGame";
import { hintsFrom, mapHint } from "../common/hints";
import { bothFirstLettersHint } from "./pairHints";

export default defineGame({
  type: "MERGED_COUNTRIES",
  title: "Merged Countries",
  singular: "Merged countries",
  description: "Name the two neighbouring countries merged into one shape.",
  Icon: Union,
  hasClues: true,
  Component: makePairGame({
    MapComponent: MergedCountriesMapWrapper,
    prompt: "Which two neighbouring countries make this shape?",
    mapKey: () => "They are filled in as one shape. The border between them isn't drawn at first.",
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
  answerNames: (round) => round.countries.map((country) => country.name),
});
