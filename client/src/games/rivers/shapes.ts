import { defineGame } from "../types";
import { makeRiverGame } from "./RiverGame";
import RiverShapesMapWrapper from "./RiverShapesMapWrapper";
import { continentHint, mainCountryHint, firstLetterHint, textHints } from "./riverHints";
import { RiverIcon } from "../../ui/icons";

export default defineGame({
  type: "RIVERS_BY_SHAPE",
  description: "Name a river from its shape alone.",
  Icon: RiverIcon,
  extraDifficult: true,
  hasClues: true,
  Component: makeRiverGame({
    MapComponent: RiverShapesMapWrapper,
    prompt: "Which river is this?",
    blankMap: true,
    hints: (river, continentShown) =>
      textHints(river, ...(continentShown ? [] : [continentHint]), mainCountryHint, firstLetterHint),
  }),
  answers: (round) => [round.river],
});
