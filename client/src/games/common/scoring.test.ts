import { describe, expect, it } from "vitest";
import { roundPoints } from "./scoring";

// The same tables are in the backend's ScoringTest. Keep them identical.
const correctTable: [hintsUsed: number, wrongGuesses: number, points: number][] = [
  [0, 0, 10],
  [0, 1, 9],
  [0, 3, 7],
  [0, 6, 7],
  [1, 0, 9],
  [3, 0, 7],
  [2, 2, 6],
  [3, 4, 4],
];

const revealedTable: [hintsUsed: number, wrongGuesses: number, answerCount: number, found: number, points: number][] = [
  [0, 1, 3, 2, 5],
  [0, 1, 3, 1, 2],
  [0, 1, 2, 1, 4],
  [0, 1, 3, 0, 0],
  [3, 1, 3, 1, 1],
  [1, 1, 2, 1, 3],
];

describe("roundPoints", () => {
  it.each(correctTable)("a correct round with %i hints and %i wrong guesses is %i", (hints, wrong, points) => {
    expect(roundPoints("CORRECT", hints, 1, 1, wrong)).toBe(points);
  });

  it.each(revealedTable)(
    "a revealed round with %i hints, %i wrong, %i answers and %i found is %i",
    (hints, wrong, answerCount, found, points) => {
      expect(roundPoints("GAVE_UP", hints, found, answerCount, wrong)).toBe(points);
      expect(roundPoints("OUT_OF_GUESSES", hints, found, answerCount, wrong)).toBe(points);
    },
  );

  it("ignores the found count for a correct or unanswered round", () => {
    expect(roundPoints("CORRECT", 1, 0, 3, 0)).toBe(9);
    expect(roundPoints("UNANSWERED", 0, 2, 3, 0)).toBe(0);
  });

  it("gives a revealed round nothing for no misses", () => {
    expect(roundPoints("GAVE_UP", 0, 1, 1, 0)).toBe(7);
  });
});
