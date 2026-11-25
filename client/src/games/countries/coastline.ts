import { defineGame } from "../types";
import { makeCountryGame } from "./CountryGame";
import type { CountryShapeRound } from "./model";
import CoastlineMapWrapper from "./CoastlineMapWrapper";
import { hintsFrom, mapHint } from "../common/hints";
import { firstLetterHint, longestBorderHint } from "./countryHints";
import { CoastlineIcon } from "../../ui/icons";

export default defineGame({
  type: "COUNTRIES_BY_COASTLINE",
  description: "Name a country from its coastline alone.",
  Icon: CoastlineIcon,
  hasClues: true,
  Component: makeCountryGame<CountryShapeRound>({
    MapComponent: CoastlineMapWrapper,
    prompt: "Which country has this coastline?",
    hints: (round) =>
      hintsFrom(
        "Shows its land borders, then names its longest border, then its first letter",
        round,
        // The first hint changes the map; it only says so.
        mapHint("Its land borders are on the map"),
        longestBorderHint,
        firstLetterHint,
      ),
  }),
  answers: (round) => [round.country],
});
