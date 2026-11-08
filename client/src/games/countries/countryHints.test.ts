import { describe, expect, it } from "vitest";
import { makeCountryShapeData, makeCountryShapeRoundData } from "../../test/fixtures";
import { countryHints, firstLetterHint, mapHint, neighbourCountHint, neighbourNamesHint } from "./countryHints";

describe("countryHints", () => {
  const round = makeCountryShapeRoundData();

  it("counts every neighbour, overseas ones included", () => {
    expect(neighbourCountHint(round)).toBe("Borders 3 countries");
    const one = makeCountryShapeRoundData({
      country: makeCountryShapeData({ neighbours: [{ name: "Spain", km: 1, mainland: true }] }),
    });
    expect(neighbourCountHint(one)).toBe("Borders 1 country");
  });

  it("names only the mainland neighbours, longest border first", () => {
    expect(neighbourNamesHint(round)).toBe("Borders: Germany, Belgium");
  });

  it("names at most three and counts the rest", () => {
    const many = ["A", "B", "C", "D", "E"].map((name) => ({ name, km: 1, mainland: true }));
    const crowded = makeCountryShapeRoundData({ country: makeCountryShapeData({ neighbours: many }) });

    expect(neighbourNamesHint(crowded)).toBe("Borders: A, B, C and 2 more");
  });

  it("has nothing to name for an island", () => {
    const island = makeCountryShapeRoundData({ country: makeCountryShapeData({ neighbours: [] }) });

    expect(neighbourNamesHint(island)).toBeNull();
    expect(neighbourCountHint(island)).toBe("Borders 0 countries");
  });

  it("gives the first letter", () => {
    expect(firstLetterHint(round)).toBe("Starts with “N”");
  });

  it("mapHint always says the same thing", () => {
    expect(mapHint("On the map")(round)).toBe("On the map");
  });

  it("makes one hint per builder, in order", () => {
    const hints = countryHints("Shows clues", round, mapHint("A"), firstLetterHint);

    expect(hints.description).toBe("Shows clues");
    expect(hints.text?.(0)).toBe("A");
    expect(hints.text?.(1)).toBe("Starts with “N”");
    expect(hints.text?.(2)).toBeNull();
  });
});
