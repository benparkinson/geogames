import { describe, expect, it } from "vitest";
import { CORRECT_ANSWER, FINISHED, GAVE_UP, isRevealed, OUT_OF_GUESSES, UNANSWERED } from "./Model";

describe("answer state constants", () => {
  it("match the backend enum names", () => {
    expect([CORRECT_ANSWER, GAVE_UP, UNANSWERED]).toEqual(["CORRECT", "GAVE_UP", "UNANSWERED"]);
    expect(FINISHED).toBe("FINISHED");
    expect(OUT_OF_GUESSES).toBe("OUT_OF_GUESSES");
  });

  it("treats giving up and running out of guesses as revealed", () => {
    expect(isRevealed(GAVE_UP)).toBe(true);
    expect(isRevealed(OUT_OF_GUESSES)).toBe(true);
    expect(isRevealed(CORRECT_ANSWER)).toBe(false);
    expect(isRevealed(UNANSWERED)).toBe(false);
  });
});
