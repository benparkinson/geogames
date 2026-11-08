import { Globe } from "react-bootstrap-icons";
import { defineGame } from "../types";
import { makeRiverGame } from "./RiverGame";
import RiverCountriesMapWrapper from "./RiverCountriesMapWrapper";
import { firstLetterHint } from "./riverHints";

export default defineGame({
  type: "RIVERS_BY_COUNTRIES",
  title: "River Countries",
  singular: "River by countries",
  description: "Name a river from the countries it flows through.",
  Icon: Globe,
  Component: makeRiverGame({
    MapComponent: RiverCountriesMapWrapper,
    prompt: "Name the river that flows through all of these countries.",
    blankMap: true,
    // Built by hand rather than with textHints: the first hint changes the map, not the text.
    hints: (river) => ({
      description: "Shows where the river starts and ends, then its first letter",
      text: (index) => (index === 0 ? "Start and end flags are on the map" : firstLetterHint(river)),
    }),
  }),
  answerNames: (round) => [round.river.name],
});
