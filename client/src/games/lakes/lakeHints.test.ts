import { describe, expect, it } from "vitest";
import { makeLakeData } from "../../test/fixtures";
import { continentHint, firstLetterHint, lakeHints, shoresHint } from "./lakeHints";

describe("lakeHints", () => {
  const lake = makeLakeData();

  it("names the continent", () => {
    expect(continentHint(lake)).toBe("Continent: Asia");
  });

  it("says a lake with one country lies in it", () => {
    expect(shoresHint(lake)).toBe("Lies in: Russia");
  });

  it("names the countries on the shore of a bigger lake", () => {
    expect(shoresHint({ ...lake, countries: ["Kenya", "Tanzania", "Uganda"] })).toBe("Shores in: Kenya, Tanzania, Uganda");
  });

  it("counts the countries beyond the first three", () => {
    expect(shoresHint({ ...lake, countries: ["A", "B", "C", "D"] })).toBe("Shores in: A, B, C and 1 more");
  });

  it("has nothing to say about countries when there are none", () => {
    expect(shoresHint({ ...lake, countries: [] })).toBeNull();
  });

  it("gives the first letter", () => {
    expect(firstLetterHint(lake)).toBe("Starts with “B” (after “Lake”)");
  });

  it("offers the three hints in order, each with text", () => {
    const { text } = lakeHints(lake);

    expect([0, 1, 2].map((i) => text?.(i))).toEqual(["Continent: Asia", "Lies in: Russia", "Starts with “B” (after “Lake”)"]);
    expect(text?.(3)).toBeNull();
  });
});
