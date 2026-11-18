import { Water } from "react-bootstrap-icons";
import { defineGame } from "../types";
import { makeRiverGame } from "./RiverGame";
import RiverShapesMapWrapper from "./RiverShapesMapWrapper";
import { continentHint, countryCountHint, firstLetterHint, textHints } from "./riverHints";

export default defineGame({
  type: "RIVERS_BY_SHAPE",
  title: "River Shapes",
  singular: "River shape",
  description: "Name a river from its shape alone.",
  Icon: Water,
  extraDifficult: true,
  Component: makeRiverGame({
    MapComponent: RiverShapesMapWrapper,
    prompt: "Which river is this?",
    blankMap: true,
    mapKey: "The line is the river's course, with no map behind it.",
    hints: (river) => textHints(river, continentHint, countryCountHint, firstLetterHint),
  }),
  answerNames: (round) => [round.river.name],
});
