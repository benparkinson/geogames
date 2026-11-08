import { describe, expect, it } from "vitest";
import { isPerfect, roundPoints } from "./scoring";

// The same table is in the backend's ScoringTest. Keep them identical.
const table: [hintCount: number, hintsUsed: number, correct: number, gaveUp: number][] = [
  [0, 0, 100, 0],
  [1, 0, 100, 0],
  [1, 1, 50, 0],
  [2, 1, 75, 0],
  [2, 2, 50, 0],
  [3, 1, 83, 0],
  [3, 2, 67, 0],
  [3, 3, 50, 0],
];

describe("roundPoints", () => {
  it.each(table)("with %i hints and %i used: correct is %i, revealed is %i", (hintCount, hintsUsed, correct, gaveUp) => {
    expect(roundPoints("CORRECT", hintsUsed, hintCount)).toBe(correct);
    expect(roundPoints("GAVE_UP", hintsUsed, hintCount)).toBe(gaveUp);
    expect(roundPoints("OUT_OF_GUESSES", hintsUsed, hintCount)).toBe(gaveUp);
    expect(roundPoints("UNANSWERED", hintsUsed, hintCount)).toBe(0);
  });
});

describe("isPerfect", () => {
  it("is a correct answer without hints", () => {
    expect(isPerfect("CORRECT", 0)).toBe(true);
    expect(isPerfect("CORRECT", 1)).toBe(false);
    expect(isPerfect("GAVE_UP", 0)).toBe(false);
    expect(isPerfect("UNANSWERED", 0)).toBe(false);
  });
});
