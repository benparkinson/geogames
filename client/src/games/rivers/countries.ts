import { Globe } from "react-bootstrap-icons";
import { defineGame } from "../types";
import { makeRiverGame } from "./RiverGame";
import RiverCountriesMapWrapper from "./RiverCountriesMapWrapper";
import { hintsFrom, mapHint } from "../common/hints";
import { firstLetterHint } from "./riverHints";

export default defineGame({
  type: "RIVERS_BY_COUNTRIES",
  title: "River Countries",
  singular: "River by countries",
  description: "Name a river from the countries it flows through.",
  Icon: Globe,
  Component: makeRiverGame({
    MapComponent: RiverCountriesMapWrapper,
    prompt: "Which river flows through all of these countries?",
    mapKey: "The shaded countries are the ones it crosses. The river itself isn't drawn.",
    blankMap: true,
    hints: (river) =>
      hintsFrom(
        "Shows where the river starts and ends, then its first letter",
        river,
        mapHint("Start and end flags are on the map"),
        firstLetterHint,
      ),
  }),
  answerNames: (round) => [round.river.name],
});
