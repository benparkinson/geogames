import { Water } from "react-bootstrap-icons";
import { defineGame } from "../types";
import { makeRiverGame } from "./RiverGame";
import RiverShapesMapWrapper from "./RiverShapesMapWrapper";
import { continentHint, firstLetterHint, flowsThroughHint, textHints } from "./riverHints";

export default defineGame({
  type: "RIVERS_BY_SHAPE",
  title: "River Shapes",
  singular: "River shape",
  description: "Name a river from its shape alone.",
  Icon: Water,
  extraDifficult: true,
  Component: makeRiverGame({
    MapComponent: RiverShapesMapWrapper,
    prompt: "Name this river from its shape.",
    blankMap: true,
    mapCaption: "Shape only. The satellite map appears when you answer.",
    hints: (river) => textHints(river, continentHint, flowsThroughHint, firstLetterHint),
  }),
  answerNames: (round) => [round.river.name],
});
