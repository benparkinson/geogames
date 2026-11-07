import { describe, expect, it } from "vitest";
import { GAME_OPTIONS, ROUND_OPTIONS } from "./gameTypes";

describe("GAME_OPTIONS", () => {
  it("uses the names the backend understands", () => {
    expect(GAME_OPTIONS.map((o) => o.name)).toEqual(["TRIPOINT", "RIVERS_BY_SHAPE", "RANDOM"]);
  });

  it("gives every option a title, description and icon", () => {
    for (const option of GAME_OPTIONS) {
      expect(option.title).not.toBe("");
      expect(option.singular).not.toBe("");
      expect(option.description).not.toBe("");
      expect(option.Icon).toBeDefined();
    }
  });
});

describe("ROUND_OPTIONS", () => {
  it("stays within the backend limit of 1 to 10 rounds", () => {
    for (const rounds of ROUND_OPTIONS) {
      expect(rounds).toBeGreaterThanOrEqual(1);
      expect(rounds).toBeLessThanOrEqual(10);
    }
  });
});
