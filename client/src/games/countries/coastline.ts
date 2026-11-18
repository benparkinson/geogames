import { Tsunami } from "react-bootstrap-icons";
import { defineGame } from "../types";
import { makeCountryGame } from "./CountryGame";
import type { CountryShapeRound } from "./model";
import CoastlineMapWrapper from "./CoastlineMapWrapper";
import { hintsFrom, mapHint } from "../common/hints";
import { firstLetterHint, neighbourCountHint } from "./countryHints";

export default defineGame({
  type: "COUNTRIES_BY_COASTLINE",
  title: "Coastlines",
  singular: "Country by coastline",
  description: "Name a country from its coastline alone.",
  Icon: Tsunami,
  hasClues: true,
  Component: makeCountryGame<CountryShapeRound>({
    MapComponent: CoastlineMapWrapper,
    prompt: "Which country has this coastline?",
    mapKey: "The solid lines are its coast. Its land borders aren't drawn at first.",
    hints: (round) =>
      hintsFrom(
        "Shows how many countries its mainland borders, then its land borders, then its first letter",
        round,
        neighbourCountHint,
        // The second hint changes the map; it only says so.
        mapHint("Its land borders are on the map"),
        firstLetterHint,
      ),
  }),
  answerNames: (round) => [round.country.name],
});
