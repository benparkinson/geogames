import { BoundingBox } from "react-bootstrap-icons";
import { defineGame } from "../types";
import { makeCountryGame } from "./CountryGame";
import type { CountryShapeRound } from "./model";
import LandBordersMapWrapper from "./LandBordersMapWrapper";
import { hintsFrom, mapHint } from "../common/hints";
import { firstLetterHint } from "./countryHints";

export default defineGame({
  type: "COUNTRIES_BY_LAND_BORDERS",
  title: "Land Borders",
  singular: "Country by land borders",
  description: "Name a country from its land borders alone.",
  Icon: BoundingBox,
  hasClues: true,
  Component: makeCountryGame<CountryShapeRound>({
    MapComponent: LandBordersMapWrapper,
    prompt: "Name the country with these land borders.",
    mapCaption: "Land borders only. The coastline is hidden.",
    // The first two hints change the map; they only say so.
    hints: (round) =>
      hintsFrom(
        "Shows its neighbours, then its coastline, then its first letter",
        round,
        mapHint("Its neighbours are on the map"),
        mapHint("Its coastline is on the map"),
        firstLetterHint,
      ),
  }),
  answerNames: (round) => [round.country.name],
});
