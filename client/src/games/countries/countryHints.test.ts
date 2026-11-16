import { describe, expect, it } from "vitest";
import { makeCountryShapeData, makeCountryShapeRoundData } from "../../test/fixtures";
import {
  borderLengthHint,
  countryHints,
  firstLetterHint,
  longestBorderHint,
  mapHint,
  neighbourCountHint,
}  from "./countryHints";

describe("countryHints", () => {
  const round = makeCountryShapeRoundData();

  it("counts only the mainland neighbours, not those of overseas territories", () => {
    expect(neighbourCountHint(round)).toBe("Its mainland borders 2 countries");
    const one = makeCountryShapeRoundData({
      country: makeCountryShapeData({
        neighbours: [
          { name: "Spain", km: 1, mainland: true },
          { name: "Brazil", km: 1, mainland: false },
        ],
      }),
    });
    expect(neighbourCountHint(one)).toBe("Its mainland borders 1 country");
  });

  it("names the mainland neighbour with the longest border", () => {
    expect(longestBorderHint(round)).toBe("Longest border: Germany");
    const overseasFirst = makeCountryShapeRoundData({
      country: makeCountryShapeData({
        neighbours: [
          { name: "Brazil", km: 9, mainland: false },
          { name: "Spain", km: 1, mainland: true },
        ],
      }),
    });
    expect(longestBorderHint(overseasFirst)).toBe("Longest border: Spain");
  });

  it("says how long the borders of a landlocked country are", () => {
    expect(borderLengthHint(round)).toBe("No coastline. Its borders are 1,027 km long");
  });

  it("has nothing to name for an island", () => {
    const island = makeCountryShapeRoundData({ country: makeCountryShapeData({ neighbours: [] }) });

    expect(longestBorderHint(island)).toBeNull();
    expect(neighbourCountHint(island)).toBe("Its mainland borders 0 countries");
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
