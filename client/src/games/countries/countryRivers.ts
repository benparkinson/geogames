import { Water } from "react-bootstrap-icons";
import { defineGame } from "../types";
import { makeCountryGame } from "./CountryGame";
import type { CountryRiversRound } from "./model";
import CountryRiversMapWrapper from "./CountryRiversMapWrapper";
import { hintsFrom } from "../common/hints";
import { coastHint, firstLetterHint, lakesHint } from "./countryHints";

export default defineGame({
  type: "COUNTRIES_BY_RIVERS",
  title: "Country Rivers",
  singular: "Country by rivers",
  description: "Name a country from the rivers inside it.",
  Icon: Water,
  hasClues: true,
  Component: makeCountryGame<CountryRiversRound>({
    MapComponent: CountryRiversMapWrapper,
    prompt: "Which country has these rivers?",
    // The first two hints change the map; they only say so.
    hints: (round) =>
      hintsFrom(
        "Shows its lakes, then its coastline, then its first letter",
        round,
        lakesHint,
        coastHint,
        firstLetterHint,
      ),
  }),
  answerNames: (round) => [round.country.name],
});
