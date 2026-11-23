import { defineGame } from "../types";
import { makeCountryGame } from "./CountryGame";
import type { CountryCitiesRound } from "./model";
import CountryCitiesMapWrapper from "./CountryCitiesMapWrapper";
import { hintsFrom } from "../common/hints";
import { capitalHint, coastHint, firstLetterHint } from "./countryHints";
import { CitiesIcon } from "../../ui/icons";

export default defineGame({
  type: "COUNTRIES_BY_CITIES",
  title: "Country Cities",
  singular: "Country by cities",
  description: "Name a country from where its cities are.",
  Icon: CitiesIcon,
  hasClues: true,
  Component: makeCountryGame<CountryCitiesRound>({
    MapComponent: CountryCitiesMapWrapper,
    prompt: "Which country has these cities?",
    // The first two hints change the map; they only say so.
    hints: (round) =>
      hintsFrom(
        "Highlights its capital, then shows its coastline, then its first letter",
        round,
        capitalHint,
        coastHint,
        firstLetterHint,
      ),
  }),
  answers: (round) => [round.country],
});
