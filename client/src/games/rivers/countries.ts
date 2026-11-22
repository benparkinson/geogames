import { defineGame } from "../types";
import { makeRiverGame } from "./RiverGame";
import RiverCountriesMapWrapper from "./RiverCountriesMapWrapper";
import { hintsFrom, mapHint } from "../common/hints";
import { firstLetterHint } from "./riverHints";
import { ContinentIcon } from "../../ui/icons";

export default defineGame({
  type: "RIVERS_BY_COUNTRIES",
  title: "River Countries",
  singular: "River by countries",
  description: "Name a river from the countries it flows through.",
  Icon: ContinentIcon,
  Component: makeRiverGame({
    MapComponent: RiverCountriesMapWrapper,
    prompt: "Which river flows through all of these countries?",
    blankMap: true,
    hints: (river) =>
      hintsFrom(
        "Shows where the river starts and ends, then its first letter",
        river,
        mapHint("The start and end are marked on the map"),
        firstLetterHint,
      ),
  }),
  answerNames: (round) => [round.river.name],
});
