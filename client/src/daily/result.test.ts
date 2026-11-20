import { describe, expect, it } from "vitest";
import type { GameRoundView } from "../api/types";
import { resultFromGame } from "./result";

const base: Pick<
  GameRoundView["gameResult"],
  | "gameState"
  | "score"
  | "maxScore"
  | "roundsCorrect"
  | "perfectRounds"
  | "roundPoints"
  | "roundFirstGuess"
  | "firstGuessRounds"
> = {
  gameState: "FINISHED",
  score: 233,
  maxScore: 400,
  roundsCorrect: 3,
  perfectRounds: 1,
  roundPoints: [100, 83, 50, 0],
  roundFirstGuess: [true, false, false, false],
  firstGuessRounds: 1,
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
    expect(
      resultFromGame({ ...base, roundPoints: [0, 100], roundAnswerStates: ["GAVE_UP", "CORRECT"], roundHintsUsed: [2] })
        .rounds,
    ).toEqual(["revealed", "perfect"]);
  });

  it("counts a revealed round that earned points as having some, and one that earned none as revealed", () => {
    expect(
      resultFromGame({
        ...base,
        roundPoints: [67, 0, 25],
        roundAnswerStates: ["GAVE_UP", "GAVE_UP", "OUT_OF_GUESSES"],
        roundHintsUsed: [0, 0, 1],
      }).rounds,
    ).toEqual(["hints", "revealed", "hints"]);
  });
});
