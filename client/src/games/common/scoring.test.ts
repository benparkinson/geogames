import { describe, expect, it } from "vitest";
import { pointsBreakdown, roundPoints } from "./scoring";

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

describe("pointsBreakdown", () => {
  const sum = (b: { answer: number; noHints: number; noMisses: number }) => b.answer + b.noHints + b.noMisses;

  it.each(correctTable)(
    "sums to the points of a correct round with %i hints and %i wrong guesses",
    (hints, wrong, points) => {
      const breakdown = pointsBreakdown("CORRECT", hints, 1, 1, wrong);
      expect(sum(breakdown)).toBe(points);
      expect(breakdown.found).toBeUndefined();
    },
  );

  it.each(revealedTable)(
    "sums to the points of a revealed round with %i hints, %i wrong, %i answers and %i found",
    (hints, wrong, answerCount, found, points) => {
      const breakdown = pointsBreakdown("GAVE_UP", hints, found, answerCount, wrong);
      expect(sum(breakdown)).toBe(points);
      expect(breakdown.noMisses).toBe(0);
    },
  );

  it("splits a correct round into its three parts", () => {
    expect(pointsBreakdown("CORRECT", 1, 1, 1, 2)).toEqual({ answer: 4, noHints: 2, noMisses: 1 });
  });

  it("fills the Answer part first for a revealed round, then No hints, and names how many were found", () => {
    expect(pointsBreakdown("GAVE_UP", 0, 2, 3, 0)).toEqual({
      answer: 4,
      noHints: 1,
      noMisses: 0,
      found: { found: 2, count: 3 },
    });
    expect(pointsBreakdown("OUT_OF_GUESSES", 0, 1, 3, 2)).toEqual({
      answer: 2,
      noHints: 0,
      noMisses: 0,
      found: { found: 1, count: 3 },
    });
  });

  it("is empty for an unanswered round", () => {
    expect(pointsBreakdown("UNANSWERED", 0, 0, 1, 0)).toEqual({ answer: 0, noHints: 0, noMisses: 0 });
  });
});
