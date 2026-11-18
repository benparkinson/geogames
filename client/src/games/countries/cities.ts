import { Buildings } from "react-bootstrap-icons";
import { defineGame } from "../types";
import { makeCountryShapeGame } from "./CountryShapeGame";
import type { CountryCitiesRound } from "./model";
import CountryCitiesMapWrapper from "./CountryCitiesMapWrapper";
import { hintsFrom } from "../common/hints";
import { capitalHint, coastHint, firstLetterHint } from "./countryHints";

export default defineGame({
  type: "COUNTRIES_BY_CITIES",
  title: "Country Cities",
  singular: "Country by cities",
  description: "Name a country from where its cities are.",
  Icon: Buildings,
  hasClues: true,
  Component: makeCountryShapeGame<CountryCitiesRound>({
    MapComponent: CountryCitiesMapWrapper,
    prompt: "Name the country with these cities.",
    mapCaption: "Its cities, sized by population. The outline is hidden.",
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
  answerNames: (round) => [round.country.name],
});
