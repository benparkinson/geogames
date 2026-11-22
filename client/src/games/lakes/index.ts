import { defineGame } from "../types";
import LakeGame from "./LakeGame";
import type { LakeRound } from "./model";
import { LakeIcon } from "../../ui/icons";

export default defineGame<LakeRound>({
  type: "LAKES_BY_SHAPE",
  title: "Lake Shapes",
  singular: "Lake shape",
  description: "Name a lake from its shape alone.",
  Icon: LakeIcon,
  extraDifficult: true,
  Component: LakeGame,
  answerNames: (round) => [round.lake.name],
});
