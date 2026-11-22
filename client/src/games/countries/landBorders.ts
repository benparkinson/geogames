import { defineGame } from "../types";
import { makeCountryGame } from "./CountryGame";
import type { CountryShapeRound } from "./model";
import LandBordersMapWrapper from "./LandBordersMapWrapper";
import { hintsFrom, mapHint } from "../common/hints";
import { firstLetterHint } from "./countryHints";
import { LandBordersIcon } from "../../ui/icons";

export default defineGame({
  type: "COUNTRIES_BY_LAND_BORDERS",
  title: "Land Borders",
  singular: "Country by land borders",
  description: "Name a country from its land borders alone.",
  Icon: LandBordersIcon,
  hasClues: true,
  Component: makeCountryGame<CountryShapeRound>({
    MapComponent: LandBordersMapWrapper,
    prompt: "Which country has these land borders?",
    // The first two hints change the map; they only say so.
    hints: (round) =>
      hintsFrom(
        "Shows its coastline, then its neighbours, then its first letter",
        round,
        mapHint("Its coastline is on the map"),
        mapHint("Its neighbours are on the map"),
        firstLetterHint,
      ),
  }),
  answerNames: (round) => [round.country.name],
});
