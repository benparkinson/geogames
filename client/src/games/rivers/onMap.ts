import { Map as MapIcon } from "react-bootstrap-icons";
import { defineGame } from "../types";
import { makeRiverGame } from "./RiverGame";
import RiverOnMapMapWrapper from "./RiverOnMapMapWrapper";
import { firstLetterHint, flowsThroughHint, textHints } from "./riverHints";

export default defineGame({
  type: "RIVERS_ON_MAP",
  title: "Rivers on the Map",
  singular: "River on map",
  description: "Name a river drawn on a satellite map.",
  Icon: MapIcon,
  Component: makeRiverGame({
    MapComponent: RiverOnMapMapWrapper,
    prompt: "Name the river shown on the map.",
    mapCaption: "No panning: zoom is limited.",
    hints: (river) => textHints(river, flowsThroughHint, firstLetterHint),
  }),
  answerNames: (round) => [round.river.name],
});
