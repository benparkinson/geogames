import { PuzzleFill } from "react-bootstrap-icons";
import { defineGame } from "../types";
import { makeCountryShapeGame } from "./CountryShapeGame";
import NeighboursMapWrapper from "./NeighboursMapWrapper";
import { countryHints, firstLetterHint, mapHint, neighbourNamesHint } from "./countryHints";

export default defineGame({
  type: "COUNTRIES_BY_NEIGHBOURS",
  title: "Country Gaps",
  singular: "Country by neighbours",
  description: "Name the country hidden between its neighbours.",
  Icon: PuzzleFill,
  Component: makeCountryShapeGame({
    MapComponent: NeighboursMapWrapper,
    prompt: "Name the missing country.",
    mapCaption: "Only its neighbours are shown. The country is the gap.",
    hints: (round) =>
      countryHints(
        "Shows its coastline, then which countries it borders, then its first letter",
        round,
        // The first hint changes the map; it only says so.
        round.country.coastKm === 0 ? mapHint("It has no coastline") : mapHint("Its coastline is on the map"),
        neighbourNamesHint,
        firstLetterHint,
      ),
  }),
  answerNames: (round) => [round.country.name],
});
