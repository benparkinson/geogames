import { describe, expect, it } from "vitest";
import type { GameRoundView } from "../api/types";
import { resultFromGame } from "./result";

const base: Pick<
  GameRoundView["gameResult"],
  "gameState" | "score" | "maxScore" | "roundsCorrect" | "perfectRounds" | "roundPoints"
> = {
  gameState: "FINISHED",
  score: 233,
  maxScore: 400,
  roundsCorrect: 3,
  perfectRounds: 1,
  roundPoints: [100, 83, 50, 0],
};

describe("resultFromGame", () => {
  it("tells perfect, hinted and revealed rounds apart", () => {
    expect(
      resultFromGame({
        ...base,
        roundAnswerStates: ["CORRECT", "CORRECT", "CORRECT", "OUT_OF_GUESSES"],
        roundHintsUsed: [0, 1, 3, 0],
      }),
    ).toEqual({ score: 233, maxScore: 400, rounds: ["perfect", "hints", "hints", "revealed"] });
  });

  it("counts a given up round as revealed, and treats a missing hint count as none", () => {
    expect(resultFromGame({ ...base, roundAnswerStates: ["GAVE_UP", "CORRECT"], roundHintsUsed: [2] }).rounds).toEqual([
      "revealed",
      "perfect",
    ]);
  });
});
