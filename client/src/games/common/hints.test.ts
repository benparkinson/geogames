import { describe, expect, it } from "vitest";
import { cluesDescription, firstLetter, firstLetterHint, hintsFrom, mapHint, nameLengthHint } from "./hints";

describe("firstLetterHint", () => {
  it("gives the upper-cased first letter", () => {
    expect(firstLetterHint("Danube")).toBe("Starts with “D”");
    expect(firstLetterHint("  rhine")).toBe("Starts with “R”");
  });

  it("skips generic leading words and says so", () => {
    expect(firstLetterHint("Lake Victoria")).toBe("Starts with “V” (after “Lake”)");
    expect(firstLetterHint("Rio Grande")).toBe("Starts with “G” (after “Rio”)");
    expect(firstLetterHint("Sea of Galilee")).toBe("Starts with “G” (after “Sea of”)");
  });

  it("keeps a generic word when it is all there is, or part of a distinctive name", () => {
    expect(firstLetterHint("Lake")).toBe("Starts with “L”");
    expect(firstLetterHint("Great Salt Lake")).toBe("Starts with “G”");
  });

  it("gives nothing for a blank name", () => {
    expect(firstLetterHint(" ")).toBeNull();
  });
});

describe("nameLengthHint", () => {
  it("counts the letters of a one-word name", () => {
    expect(nameLengthHint("Mali")).toBe("Its name has 4 letters");
    expect(nameLengthHint("X")).toBe("Its name has 1 letter");
  });

  it("counts each word of a longer name", () => {
    expect(nameLengthHint("South Sudan")).toBe("Its name has 2 words: 5 and 5 letters");
    expect(nameLengthHint("Bosnia and Herzegovina")).toBe("Its name has 3 words: 6, 3 and 11 letters");
  });

  it("splits on hyphens and counts letters only", () => {
    expect(nameLengthHint("Guinea-Bissau")).toBe("Its name has 2 words: 6 and 6 letters");
    expect(nameLengthHint("Côte d'Ivoire")).toBe("Its name has 2 words: 4 and 7 letters");
    expect(nameLengthHint("Trinidad & Tobago")).toBe("Its name has 2 words: 8 and 6 letters");
  });

  it("gives nothing for a name with no letters", () => {
    expect(nameLengthHint("")).toBeNull();
    expect(nameLengthHint(" - ")).toBeNull();
  });
});

describe("firstLetter", () => {
  it("gives the letter and the generic words skipped to reach it", () => {
    expect(firstLetter("Danube")).toEqual({ letter: "D", prefix: "" });
    expect(firstLetter("Lake Victoria")).toEqual({ letter: "V", prefix: "Lake" });
    expect(firstLetter("The Bahamas")).toEqual({ letter: "B", prefix: "The" });
  });

  it("gives nothing for a blank name", () => {
    expect(firstLetter("")).toBeNull();
  });
});

describe("mapHint", () => {
  it("always says the same thing", () => {
    expect(mapHint("On the map")("any round")).toBe("On the map");
  });
});

describe("hintsFrom", () => {
  it("makes one hint per builder, in order, and nothing past the last", () => {
    const hints = hintsFrom("Shows clues", "Nepal", mapHint("A"), mapHint("B"), firstLetterHint);

    expect(hints.description).toBe("Shows clues");
    expect(hints.text?.(0, [])).toBe("A");
    expect(hints.text?.(1, [])).toBe("B");
    expect(hints.text?.(2, [])).toBe("Starts with “N”");
    expect(hints.text?.(3, [])).toBeNull();
  });

  it("gives nothing for a builder with nothing to say", () => {
    const hints = hintsFrom("Shows a clue", " ", firstLetterHint);

    expect(hints.text?.(0, [])).toBeNull();
  });
});

describe("cluesDescription", () => {
  it("says a clue for one and counts several", () => {
    expect(cluesDescription(1)).toBe("Shows a clue");
    expect(cluesDescription(2)).toBe("Shows 2 clues, one at a time");
    expect(cluesDescription(3)).toBe("Shows 3 clues, one at a time");
  });
});
