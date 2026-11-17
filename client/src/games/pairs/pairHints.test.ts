import { describe, expect, it } from "vitest";
import { makeCountryPairRoundData, makeCountryShapeData } from "../../test/fixtures";
import { bothFirstLettersHint, mapHint, pairHints } from "./pairHints";

describe("pairHints", () => {
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

  it("mapHint always says the same thing", () => {
    expect(mapHint("On the map")(round)).toBe("On the map");
  });

  it("gives one hint per builder, revealed in order, and nothing past the last", () => {
    const hints = pairHints("Three hints", round, mapHint("one"), mapHint("two"), bothFirstLettersHint);

    expect(hints.description).toBe("Three hints");
    expect(hints.text?.(0)).toBe("one");
    expect(hints.text?.(1)).toBe("two");
    expect(hints.text?.(2)).toBe("Their names start with “B” and “N”");
    expect(hints.text?.(3)).toBeNull();
  });
});
