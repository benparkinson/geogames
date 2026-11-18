import { PuzzleFill } from "react-bootstrap-icons";
import { defineGame } from "../types";
import { makeCountryGame } from "./CountryGame";
import type { CountryShapeRound } from "./model";
import NeighboursMapWrapper from "./NeighboursMapWrapper";
import { hintsFrom, mapHint } from "../common/hints";
import { borderLengthHint, firstLetterHint, longestBorderHint } from "./countryHints";

export default defineGame({
  type: "COUNTRIES_BY_NEIGHBOURS",
  title: "Country Gaps",
  singular: "Country by neighbours",
  description: "Name the country hidden between its neighbours.",
  Icon: PuzzleFill,
  hasClues: true,
  Component: makeCountryGame<CountryShapeRound>({
    MapComponent: NeighboursMapWrapper,
    prompt: "Name the missing country.",
    mapCaption: "Only its neighbours are shown. The country is the gap.",
    hints: (round) =>
      hintsFrom(
        round.country.coastKm === 0
          ? "Shows how long its borders are, then its longest border, then its first letter"
          : "Shows its coastline, then its longest border, then its first letter",
        round,
        // With a coast, the first hint changes the map; it only says so.
        round.country.coastKm === 0 ? borderLengthHint : mapHint("Its coastline is on the map"),
        longestBorderHint,
        firstLetterHint,
      ),
  }),
  answerNames: (round) => [round.country.name],
});
