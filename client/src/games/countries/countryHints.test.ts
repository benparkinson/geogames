import { describe, expect, it } from "vitest";
import { makeCountryShapeData, makeCountryShapeRoundData } from "../../test/fixtures";
import {
  capitalHint,
  coastHint,
  firstLetterHint,
  longestBorderHint,
  longestRiverHint,
  nameLengthHint,
  namedLongestRiver,
} from "./countryHints";

describe("country hint builders", () => {
  const round = makeCountryShapeRoundData();

  it("names the mainland neighbour with the longest border", () => {
    expect(longestBorderHint(round)).toBe("Longest border: Germany");
    const overseasFirst = makeCountryShapeRoundData({
      country: makeCountryShapeData({
        neighbours: [
          { name: "Brazil", km: 9, mainland: false, lines: [] },
          { name: "Spain", km: 1, mainland: true, lines: [] },
        ],
      }),
    });
    expect(longestBorderHint(overseasFirst)).toBe("Longest border: Spain");
  });

  it("falls back to a neighbour overseas when there is no mainland one", () => {
    const uk = makeCountryShapeRoundData({
      country: makeCountryShapeData({ neighbours: [{ name: "Ireland", km: 499, mainland: false, lines: [] }] }),
    });

    expect(longestBorderHint(uk)).toBe("Longest border: Ireland");
  });

  it("says how long the name is", () => {
    expect(nameLengthHint(round)).toBe("Its name has 11 letters");
  });

  it("has nothing to name for an island", () => {
    const island = makeCountryShapeRoundData({ country: makeCountryShapeData({ neighbours: [] }) });

    expect(longestBorderHint(island)).toBeNull();
  });

  it("gives the first letter", () => {
    expect(firstLetterHint(round)).toBe("Starts with “N”");
  });
});

describe("coastHint", () => {
  it("says the coastline is on the map", () => {
    expect(coastHint(makeCountryShapeRoundData())).toBe("Its coastline is on the map");
  });

  it("says a landlocked country has none", () => {
    const landlocked = makeCountryShapeRoundData({ country: makeCountryShapeData({ coastKm: 0 }) });

    expect(coastHint(landlocked)).toBe("It has no coastline");
  });
});

describe("longestRiverHint", () => {
  const country = makeCountryShapeData();

  it("names the longest river", () => {
    expect(longestRiverHint({ country, longestRiver: "Danube" })).toBe("Its longest river is the Danube");
  });

  it("falls back to the length of the name when the river's name shows the country's", () => {
    const nigeria = { ...country, name: "Nigeria", additionalNames: [] };
    expect(longestRiverHint({ country: nigeria, longestRiver: "Niger" })).toBe("Its name has 7 letters");
    expect(namedLongestRiver({ country: nigeria, longestRiver: "Niger" })).toBeNull();
    expect(namedLongestRiver({ country, longestRiver: "Danube" })).toBe("Danube");
  });

  it("falls back to the length of the name when the country has no named river", () => {
    expect(longestRiverHint({ country, longestRiver: null })).toBe("Its name has 11 letters");
  });
});

describe("capitalHint", () => {
  const city = (name: string, capital: boolean) => ({ name, lon: 0, lat: 0, population: 1, capital });
  const country = makeCountryShapeData();

  it("says the capital is highlighted", () => {
    expect(capitalHint({ country, cities: [city("A", true), city("B", false)] })).toBe("Its capital is highlighted");
  });

  it("says capitals when a country has more than one", () => {
    expect(capitalHint({ country, cities: [city("A", true), city("B", true)] })).toBe("Its capitals are highlighted");
  });
});
