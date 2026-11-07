import { Water } from "react-bootstrap-icons";
import { defineGame } from "../types";
import RiverShapesGame from "./RiverShapesGame";

export default defineGame({
  type: "RIVERS_BY_SHAPE",
  title: "Rivers",
  singular: "River",
  description: "Name a river from its shape alone.",
  Icon: Water,
  Component: RiverShapesGame,
  answerNames: (river) => [river.name],
});
