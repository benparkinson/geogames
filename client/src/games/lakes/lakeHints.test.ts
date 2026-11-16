import { describe, expect, it } from "vitest";
import { makeLakeData } from "../../test/fixtures";
import { continentHint, countryCountHint, firstLetterHint, lakeHints } from "./lakeHints";

describe("lakeHints", () => {
  const lake = makeLakeData();

  it("names the continent", () => {
    expect(continentHint(lake)).toBe("Continent: Asia");
  });

  it("says a lake with one country lies in it", () => {
    expect(countryCountHint(lake)).toBe("Lies in 1 country");
  });

  it("counts the countries on the shore of a bigger lake without naming them", () => {
    expect(countryCountHint({ ...lake, countries: ["Kenya", "Tanzania", "Uganda"] })).toBe("Shores in 3 countries");
  });

  it("gives the first letter", () => {
    expect(firstLetterHint(lake)).toBe("Starts with “B” (after “Lake”)");
  });

  it("offers the three hints in order, each with text", () => {
    const { text } = lakeHints(lake);

    expect([0, 1, 2].map((i) => text?.(i))).toEqual(["Continent: Asia", "Lies in 1 country", "Starts with “B” (after “Lake”)"]);
    expect(text?.(3)).toBeNull();
  });
});
