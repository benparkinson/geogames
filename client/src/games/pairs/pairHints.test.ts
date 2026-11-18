import { describe, expect, it } from "vitest";
import { makeCountryPairRoundData, makeCountryShapeData } from "../../test/fixtures";
import { bothFirstLettersHint } from "./pairHints";

describe("pair hint builders", () => {
  const round = makeCountryPairRoundData();

  it("gives both first letters in name order", () => {
    expect(bothFirstLettersHint(round)).toBe("Their names start with “B” and “N”");
  });

  it("skips a generic leading word, like the first-letter hint of the other games", () => {
    const bahamas = makeCountryPairRoundData({
      countries: [makeCountryShapeData({ name: "The Bahamas" }), makeCountryShapeData({ name: "Cuba" })],
    });

    expect(bothFirstLettersHint(bahamas)).toBe("Their names start with “B” and “C”");
  });

  it("has nothing to say about a blank name", () => {
    const blank = makeCountryPairRoundData({
      countries: [makeCountryShapeData({ name: " " }), makeCountryShapeData({ name: "Cuba" })],
    });

    expect(bothFirstLettersHint(blank)).toBeNull();
  });
});
