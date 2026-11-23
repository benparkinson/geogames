import { describe, expect, it } from "vitest";
import { OUTCOME_STYLES, roundOutcome } from "./outcome";

describe("roundOutcome", () => {
  it("is null while unanswered", () => {
    expect(roundOutcome("UNANSWERED", 0, false, 0)).toBeNull();
  });

  it("is a first guess when correct with no hints and no wrong guess", () => {
    expect(roundOutcome("CORRECT", 0, true, 100)).toBe("firstGuess");
  });

  it("is correct after a wrong guess, or when the first-guess flag is unknown", () => {
    expect(roundOutcome("CORRECT", 0, false, 100)).toBe("correct");
  });

  it("is with hints when correct with a hint, even with no wrong guess", () => {
    expect(roundOutcome("CORRECT", 1, true, 83)).toBe("hints");
    expect(roundOutcome("CORRECT", 2, false, 67)).toBe("hints");
  });

  it("is missed when revealed with no points", () => {
    expect(roundOutcome("GAVE_UP", 0, false, 0)).toBe("missed");
    expect(roundOutcome("OUT_OF_GUESSES", 2, false, 0)).toBe("missed");
  });

  it("is with hints when revealed with some answers found: giving up is not penalised", () => {
    expect(roundOutcome("GAVE_UP", 0, false, 67)).toBe("hints");
    expect(roundOutcome("OUT_OF_GUESSES", 0, false, 33)).toBe("hints");
  });
});

describe("OUTCOME_STYLES", () => {
  it("defines every outcome, and no label says perfect", () => {
    expect(Object.keys(OUTCOME_STYLES).sort()).toEqual(["correct", "firstGuess", "hints", "missed"]);
    expect(JSON.stringify(OUTCOME_STYLES).toLowerCase()).not.toContain("perfect");
  });

  it("uses the share squares of the plan", () => {
    expect((["firstGuess", "correct", "hints", "missed"] as const).map((o) => OUTCOME_STYLES[o].emoji).join("")).toBe(
      "❇️🟩🟨🟥",
    );
  });
});
