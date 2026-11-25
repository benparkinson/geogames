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
  score: 25,
  maxScore: 40,
  roundsCorrect: 3,
  roundPoints: [10, 9, 6, 0],
  roundAnswerStates: ["CORRECT", "CORRECT", "CORRECT", "OUT_OF_GUESSES"],
  roundHintsUsed: [0, 1, 3, 0],
  roundFirstGuess: [true, false, false, false],
  firstGuessRounds: 1,
};

describe("resultFromGame", () => {
  it("tells full, good, partial and missed rounds apart", () => {
    expect(
      resultFromGame({
        ...base,
        roundPoints: [10, 8, 5, 0],
        roundAnswerStates: ["CORRECT", "CORRECT", "CORRECT", "OUT_OF_GUESSES"],
        roundHintsUsed: [0, 0, 1, 0],
        roundFirstGuess: [true, false, false, false],
      }),
    ).toEqual({ score: 25, maxScore: 40, rounds: ["full", "good", "partial", "missed"] });
  });

  it("counts a given up round with no points as missed, and treats a missing hint count as none", () => {
    expect(
      resultFromGame({
        ...base,
        roundPoints: [0, 10],
        roundAnswerStates: ["GAVE_UP", "CORRECT"],
        roundHintsUsed: [2],
        roundFirstGuess: [false, true],
      }).rounds,
    ).toEqual(["missed", "full"]);
  });

  it("counts a revealed round that earned points as partial, and one that earned none as missed", () => {
    expect(
      resultFromGame({
        ...base,
        roundPoints: [5, 0, 2],
        roundAnswerStates: ["GAVE_UP", "GAVE_UP", "OUT_OF_GUESSES"],
        roundHintsUsed: [0, 0, 1],
        roundFirstGuess: [false, false, false],
      }).rounds,
    ).toEqual(["partial", "missed", "partial"]);
  });
});
