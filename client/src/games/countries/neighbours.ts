import { PuzzleFill } from "react-bootstrap-icons";
import { defineGame } from "../types";
import { makeCountryGame } from "./CountryGame";
import type { CountryShapeRound } from "./model";
import NeighboursMapWrapper from "./NeighboursMapWrapper";
import { hintsFrom, mapHint } from "../common/hints";
import { firstLetterHint, longestBorderHint, nameLengthHint } from "./countryHints";

export default defineGame({
  type: "COUNTRIES_BY_NEIGHBOURS",
  title: "Country Gaps",
  singular: "Country by neighbours",
  description: "Name the country hidden between its neighbours.",
  Icon: PuzzleFill,
  hasClues: true,
  Component: makeCountryGame<CountryShapeRound>({
    MapComponent: NeighboursMapWrapper,
    prompt: "Which country is missing?",
    hints: (round) =>
      hintsFrom(
        round.country.coastKm === 0
          ? "Shows how long its name is, then its longest border, then its first letter"
          : "Shows its coastline, then its longest border, then its first letter",
        round,
        // With a coast, the first hint changes the map; it only says so.
        round.country.coastKm === 0 ? nameLengthHint : mapHint("Its coastline is on the map"),
        longestBorderHint,
        firstLetterHint,
      ),
  }),
  answerNames: (round) => [round.country.name],
});
