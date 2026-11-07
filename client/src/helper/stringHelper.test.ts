import { describe, expect, it } from "vitest";
import { normaliseString } from "./stringHelper";

describe("normaliseString", () => {
  it("lowercases", () => {
    expect(normaliseString("FrAnCe")).toBe("france");
  });

  it("strips accents", () => {
    expect(normaliseString("Curaçao")).toBe("curacao");
    expect(normaliseString("Côte d'Ivoire")).toBe("cotedivoire");
  });

  it("strips spaces and punctuation", () => {
    expect(normaliseString("Bosnia & Herzegovina")).toBe("bosniaherzegovina");
    expect(normaliseString("St. Lucia")).toBe("stlucia");
  });

  it("keeps digits", () => {
    expect(normaliseString("Route 66")).toBe("route66");
  });

  it("turns an empty string into an empty string", () => {
    expect(normaliseString("")).toBe("");
  });

  it("is idempotent", () => {
    for (const input of ["Côte d'Ivoire", "  São Tomé & Príncipe ", "abc123"]) {
      expect(normaliseString(normaliseString(input))).toBe(normaliseString(input));
    }
  });
});
