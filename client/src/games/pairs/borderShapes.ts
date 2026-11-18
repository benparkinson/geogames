import { Bezier2 } from "react-bootstrap-icons";
import { defineGame } from "../types";
import BorderShapesMapWrapper from "./BorderShapesMapWrapper";
import { makePairGame } from "./PairGame";
import { hintsFrom, mapHint } from "../common/hints";
import { bothFirstLettersHint } from "./pairHints";

export default defineGame({
  type: "BORDERS_BY_SHAPE",
  title: "Border Shapes",
  singular: "Border",
  description: "Name the two countries a border separates, from its shape alone.",
  Icon: Bezier2,
  hasClues: true,
  extraDifficult: true,
  Component: makePairGame({
    MapComponent: BorderShapesMapWrapper,
    prompt: "Which two countries does this border separate?",
    mapKey: (round) => `The solid line is the border between them, ${round.borderKm.toLocaleString("en-GB")} km long.`,
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
  answerNames: (round) => round.countries.map((country) => country.name),
});
