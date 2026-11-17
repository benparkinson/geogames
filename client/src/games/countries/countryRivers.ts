import { Water } from "react-bootstrap-icons";
import { defineGame } from "../types";
import { makeCountryShapeGame } from "./CountryShapeGame";
import type { CountryRiversRound } from "./model";
import CountryRiversMapWrapper from "./CountryRiversMapWrapper";
import { coastHint, countryHints, firstLetterHint, lakesHint } from "./countryHints";

export default defineGame({
  type: "COUNTRIES_BY_RIVERS",
  title: "Country Rivers",
  singular: "Country by rivers",
  description: "Name a country from the rivers inside it.",
  Icon: Water,
  Component: makeCountryShapeGame<CountryRiversRound>({
    MapComponent: CountryRiversMapWrapper,
    prompt: "Name the country with these rivers.",
    mapCaption: "Its rivers, the major ones thicker. The outline is hidden.",
    // The first two hints change the map; they only say so.
    hints: (round) =>
      countryHints(
        "Shows its lakes, then its coastline, then its first letter",
        round,
        lakesHint,
        coastHint,
        firstLetterHint,
      ),
  }),
  answerNames: (round) => [round.country.name],
});
