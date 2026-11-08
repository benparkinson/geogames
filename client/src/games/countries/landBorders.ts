import { BoundingBox } from "react-bootstrap-icons";
import { defineGame } from "../types";
import { makeCountryShapeGame } from "./CountryShapeGame";
import LandBordersMapWrapper from "./LandBordersMapWrapper";
import { countryHints, firstLetterHint, mapHint } from "./countryHints";

export default defineGame({
  type: "COUNTRIES_BY_LAND_BORDERS",
  title: "Land Borders",
  singular: "Country by land borders",
  description: "Name a country from its land borders alone.",
  Icon: BoundingBox,
  Component: makeCountryShapeGame({
    MapComponent: LandBordersMapWrapper,
    prompt: "Name the country with these land borders.",
    mapCaption: "Land borders only. The coastline is hidden.",
    // The first two hints change the map; they only say so.
    hints: (round) =>
      countryHints(
        "Shows its neighbours, then its coastline, then its first letter",
        round,
        mapHint("Its neighbours are on the map"),
        mapHint("Its coastline is on the map"),
        firstLetterHint,
      ),
  }),
  answerNames: (round) => [round.country.name],
});
