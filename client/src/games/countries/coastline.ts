import { Tsunami } from "react-bootstrap-icons";
import { defineGame } from "../types";
import { makeCountryShapeGame } from "./CountryShapeGame";
import CoastlineMapWrapper from "./CoastlineMapWrapper";
import { countryHints, firstLetterHint, mapHint, neighbourCountHint } from "./countryHints";

export default defineGame({
  type: "COUNTRIES_BY_COASTLINE",
  title: "Coastlines",
  singular: "Country by coastline",
  description: "Name a country from its coastline alone.",
  Icon: Tsunami,
  Component: makeCountryShapeGame({
    MapComponent: CoastlineMapWrapper,
    prompt: "Name the country with this coastline.",
    mapCaption: "Coastline only. Land borders are hidden.",
    hints: (round) =>
      countryHints(
        "Shows how many neighbours it has, then its land borders, then its first letter",
        round,
        neighbourCountHint,
        // The second hint changes the map; it only says so.
        mapHint("Its land borders are on the map"),
        firstLetterHint,
      ),
  }),
  answerNames: (round) => [round.country.name],
});
