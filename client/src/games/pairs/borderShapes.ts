import { defineGame } from "../types";
import BorderShapesMapWrapper from "./BorderShapesMapWrapper";
import { makePairGame } from "./PairGame";
import { hintsFrom, mapHint } from "../common/hints";
import { bothFirstLettersHint } from "./pairHints";
import { BorderShapeIcon } from "../../ui/icons";

export default defineGame({
  type: "BORDERS_BY_SHAPE",
  description: "Name the two countries a border separates, from its shape alone.",
  Icon: BorderShapeIcon,
  hasClues: true,
  extraDifficult: true,
  Component: makePairGame({
    MapComponent: BorderShapesMapWrapper,
    prompt: "Which two countries does this border separate?",
    // The first two hints change the map; they only say so.
    hints: (round) =>
      hintsFrom(
        "Shows the rest of their land borders, then their coastlines, then their first letters",
        round,
        mapHint("The rest of both countries' land borders are on the map"),
        mapHint("Both coastlines are on the map"),
        bothFirstLettersHint,
      ),
  }),
  answers: (round) => round.countries,
});
