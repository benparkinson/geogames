import { describe, expect, it } from "vitest";
import { makeRiverData } from "../../test/fixtures";
import { continentHint, firstLetterHint, flowsThroughHint, textHints } from "./riverHints";

describe("riverHints", () => {
  const river = makeRiverData();

  it("names the continent", () => {
    expect(continentHint(river)).toBe("Continent: Europe");
  });

  it("names the countries it flows through", () => {
    expect(flowsThroughHint(river)).toBe("Flows through: Romania, Germany, Austria");
  });

  it("has nothing to say about countries when there are none", () => {
    expect(flowsThroughHint({ ...river, countries: [] })).toBeNull();
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
