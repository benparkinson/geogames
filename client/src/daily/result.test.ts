import { describe, expect, it } from "vitest";
import type { GameRoundView } from "../api/types";
import { resultFromGame } from "./result";

const base: Pick<
  GameRoundView["gameResult"],
  | "gameState"
  | "score"
  | "maxScore"
  | "roundsCorrect"
  | "roundPoints"
  | "roundFirstGuess"
  | "firstGuessRounds"
  | "roundAnswerStates"
  | "roundHintsUsed"
> = {
  gameState: "FINISHED",
  score: 233,
  maxScore: 400,
  roundsCorrect: 3,
  roundPoints: [100, 83, 50, 0],
  roundAnswerStates: ["CORRECT", "CORRECT", "CORRECT", "OUT_OF_GUESSES"],
  roundHintsUsed: [0, 1, 3, 0],
  roundFirstGuess: [true, false, false, false],
  firstGuessRounds: 1,
};

describe("resultFromGame", () => {
  it("tells first guess, correct, hinted and missed rounds apart", () => {
    expect(
      resultFromGame({
        ...base,
        roundPoints: [100, 100, 83, 0],
        roundAnswerStates: ["CORRECT", "CORRECT", "CORRECT", "OUT_OF_GUESSES"],
        roundHintsUsed: [0, 0, 1, 0],
        roundFirstGuess: [true, false, false, false],
      }),
    ).toEqual({ score: 233, maxScore: 400, rounds: ["firstGuess", "correct", "hints", "missed"] });
  });

  it("counts a given up round with no points as missed, and treats a missing hint count as none", () => {
    expect(
      resultFromGame({
        ...base,
        roundPoints: [0, 100],
        roundAnswerStates: ["GAVE_UP", "CORRECT"],
        roundHintsUsed: [2],
        roundFirstGuess: [false, true],
      }).rounds,
    ).toEqual(["missed", "firstGuess"]);
  });

  it("counts a revealed round that earned points as hints, and one that earned none as missed", () => {
    expect(
      resultFromGame({
        ...base,
        roundPoints: [67, 0, 25],
        roundAnswerStates: ["GAVE_UP", "GAVE_UP", "OUT_OF_GUESSES"],
        roundHintsUsed: [0, 0, 1],
        roundFirstGuess: [false, false, false],
      }).rounds,
    ).toEqual(["hints", "missed", "hints"]);
  });
});
