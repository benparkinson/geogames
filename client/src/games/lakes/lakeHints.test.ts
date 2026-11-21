import { describe, expect, it } from "vitest";
import { makeLakeData } from "../../test/fixtures";
import { continentHint, mainCountryHint, firstLetterHint, lakeHints } from "./lakeHints";

describe("lakeHints", () => {
  const lake = makeLakeData();

  it("names the continent", () => {
    expect(continentHint(lake)).toBe("Continent: Asia");
  });

  it("says a lake with one country lies entirely in it", () => {
    expect(mainCountryHint(lake)).toBe("Lies entirely in Russia");
  });

  it("names the country with the longest shoreline of a bigger lake", () => {
    expect(mainCountryHint({ ...lake, countries: ["Tanzania", "Kenya", "Uganda"] })).toBe(
      "Its longest shoreline is in Tanzania",
    );
  });

  it("gives the first letter", () => {
    expect(firstLetterHint(lake)).toBe("Starts with “B” (after “Lake”)");
  });

  it("offers the three hints in order, each with text", () => {
    const { text } = lakeHints(lake);

    expect([0, 1, 2].map((i) => text?.(i, []))).toEqual([
      "Continent: Asia",
      "Lies entirely in Russia",
      "Starts with “B” (after “Lake”)",
    ]);
    expect(text?.(3, [])).toBeNull();
  });

  it("leaves the continent out when the round already shows it", () => {
    const { text, description } = lakeHints(lake, true);

    expect([0, 1, 2].map((i) => text?.(i, []))).toEqual([
      "Lies entirely in Russia",
      "Starts with “B” (after “Lake”)",
      null,
    ]);
    expect(description).toBe("Shows 2 clues, one at a time");
  });
});
