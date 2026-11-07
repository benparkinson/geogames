import { describe, expect, it } from "vitest";
import { makeRiverData } from "../../test/fixtures";
import { continentHint, firstLetterHint, textHints } from "./riverHints";

describe("riverHints", () => {
  const river = makeRiverData();

  it("names the continent", () => {
    expect(continentHint(river)).toBe("Continent: Europe");
  });

  it("gives the first letter", () => {
    expect(firstLetterHint(river)).toBe("Starts with “D”");
    expect(firstLetterHint({ ...river, name: "  rhine" })).toBe("Starts with “R”");
  });

  it("has no first letter for a blank name", () => {
    expect(firstLetterHint({ ...river, name: " " })).toBeNull();
  });

  it("makes one hint per builder, in order", () => {
    const hints = textHints(river, continentHint, firstLetterHint);

    expect(hints.total).toBe(2);
    expect(hints.text?.(0)).toBe("Continent: Europe");
    expect(hints.text?.(1)).toBe("Starts with “D”");
    expect(hints.text?.(2)).toBeNull();
  });
});
