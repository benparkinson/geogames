import { Signpost2 } from "react-bootstrap-icons";
import { defineGame } from "../types";
import { makeRiverGame } from "./RiverGame";
import RiverEndsMapWrapper from "./RiverEndsMapWrapper";
import { firstLetterHint, flowsThroughHint, textHints } from "./riverHints";

export default defineGame({
  type: "RIVERS_BY_ENDS",
  title: "River Ends",
  singular: "River ends",
  description: "Name a river from where it starts and ends.",
  Icon: Signpost2,
  Component: makeRiverGame({
    MapComponent: RiverEndsMapWrapper,
    prompt: "Name the river that flows from the green flag to the red flag.",
    hints: (river) => textHints(river, flowsThroughHint, firstLetterHint),
  }),
  answerNames: (round) => [round.river.name],
});
