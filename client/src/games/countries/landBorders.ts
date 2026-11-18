import { BoundingBox } from "react-bootstrap-icons";
import { defineGame } from "../types";
import { makeCountryGame } from "./CountryGame";
import type { CountryShapeRound } from "./model";
import LandBordersMapWrapper from "./LandBordersMapWrapper";
import { hintsFrom, mapHint } from "../common/hints";
import { firstLetterHint } from "./countryHints";

export default defineGame({
  type: "COUNTRIES_BY_LAND_BORDERS",
  title: "Land Borders",
  singular: "Country by land borders",
  description: "Name a country from its land borders alone.",
  Icon: BoundingBox,
  hasClues: true,
  Component: makeCountryGame<CountryShapeRound>({
    MapComponent: LandBordersMapWrapper,
    prompt: "Which country has these land borders?",
    mapKey: "The solid lines are its borders with other countries. Its coast isn't drawn at first.",
    // The first two hints change the map; they only say so.
    hints: (round) =>
      hintsFrom(
        "Shows its neighbours, then its coastline, then its first letter",
        round,
        mapHint("Its neighbours are on the map"),
        mapHint("Its coastline is on the map"),
        firstLetterHint,
      ),
  }),
  answerNames: (round) => [round.country.name],
});
