import { Water } from "react-bootstrap-icons";
import { defineGame } from "../types";
import { makeCountryGame } from "./CountryGame";
import type { CountryRiversRound } from "./model";
import CountryRiversMapWrapper from "./CountryRiversMapWrapper";
import { hintsFrom } from "../common/hints";
import { coastHint, firstLetterHint, longestRiverHint } from "./countryHints";

export default defineGame({
  type: "COUNTRIES_BY_RIVERS",
  title: "Country Rivers",
  singular: "Country by rivers",
  description: "Name a country from its rivers and lakes.",
  Icon: Water,
  hasClues: true,
  Component: makeCountryGame<CountryRiversRound>({
    MapComponent: CountryRiversMapWrapper,
    prompt: "Which country has these rivers?",
    // The second hint changes the map; it only says so.
    hints: (round) =>
      hintsFrom(
        round.longestRiver
          ? "Names its longest river, then shows its coastline, then its first letter"
          : "Shows how long its name is, then its coastline, then its first letter",
        round,
        longestRiverHint,
        coastHint,
        firstLetterHint,
      ),
  }),
  answerNames: (round) => [round.country.name],
});
