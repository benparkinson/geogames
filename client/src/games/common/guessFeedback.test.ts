import { describe, expect, it } from "vitest";
import { feedbackText } from "./guessFeedback";

describe("feedbackText", () => {
  it("asks the player to try again while the names are loading", () => {
    expect(feedbackText({ kind: "loading" }, "country")).toBe("Still loading the names. Try again in a moment.");
  });
});
