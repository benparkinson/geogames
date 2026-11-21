import { describe, expect, it } from "vitest";
import { makeCountryShapeData, makeCountryShapeRoundData } from "../../test/fixtures";
import { capitalHint, coastHint, firstLetterHint, lakesHint, longestBorderHint, nameLengthHint } from "./countryHints";

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

describe("lakesHint", () => {
  const country = makeCountryShapeData();
  const lakes = (coordinates: unknown[]) =>
    ({ type: "MultiPolygon", properties: {}, geometry: { type: "MultiPolygon", coordinates } }) as never;

  it("says the lakes are on the map", () => {
    expect(lakesHint({ country, lakes: lakes([[[[0, 0]]]]) })).toBe("Its lakes are on the map");
  });

  it("says a country with none has no large lakes", () => {
    expect(lakesHint({ country, lakes: lakes([]) })).toBe("It has no large lakes");
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
