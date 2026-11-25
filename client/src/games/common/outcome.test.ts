import { describe, expect, it } from "vitest";
import { OUTCOME_STYLES, roundOutcome } from "./outcome";

describe("roundOutcome", () => {
  it("is null while unanswered", () => {
    expect(roundOutcome("UNANSWERED", 0)).toBeNull();
  });

  it.each([
    [10, "full"],
    [9, "good"],
    [7, "good"],
    [6, "partial"],
    [1, "partial"],
    [0, "missed"],
  ] as const)("is %i points -> %s", (points, outcome) => {
    expect(roundOutcome("CORRECT", points)).toBe(outcome);
  });

  it("goes by points alone for a revealed round", () => {
    expect(roundOutcome("GAVE_UP", 5)).toBe("partial");
    expect(roundOutcome("OUT_OF_GUESSES", 0)).toBe("missed");
  });
});

describe("OUTCOME_STYLES", () => {
  it("has a different square for each outcome", () => {
    const squares = Object.values(OUTCOME_STYLES).map((s) => s.emoji);
    expect(new Set(squares).size).toBe(4);
  });
});
