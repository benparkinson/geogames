import { describe, expect, it } from "vitest";
import { CORRECT_ANSWER, FINISHED, GAVE_UP, UNANSWERED } from "./Model";

describe("answer state constants", () => {
  it("match the backend enum names", () => {
    expect([CORRECT_ANSWER, GAVE_UP, UNANSWERED]).toEqual(["CORRECT", "GAVE_UP", "UNANSWERED"]);
    expect(FINISHED).toBe("FINISHED");
  });
});
