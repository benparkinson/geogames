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

  it("names all of up to three countries without saying there are more", () => {
    expect(flowsThroughHint({ ...river, countries: ["A", "B", "C"] })).toBe("Flows through: A, B, C");
    expect(flowsThroughHint({ ...river, countries: ["A"] })).toBe("Flows through: A");
  });

  it("names only the first three countries of a longer river and counts the rest", () => {
    const danube = ["Germany", "Austria", "Slovakia", "Hungary", "Croatia", "Serbia", "Romania", "Bulgaria", "Ukraine"];

    expect(flowsThroughHint({ ...river, countries: danube })).toBe("Flows through: Germany, Austria, Slovakia and 6 more");
    expect(flowsThroughHint({ ...river, countries: danube.slice(0, 4) })).toBe(
      "Flows through: Germany, Austria, Slovakia and 1 more",
    );
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

  it("describes as many clues as there are builders", () => {
    expect(textHints(river, continentHint).description).toBe("Shows a clue");
    expect(textHints(river, continentHint, firstLetterHint).description).toBe("Shows 2 clues, one at a time");
    expect(textHints(river, continentHint, flowsThroughHint, firstLetterHint).description).toBe(
      "Shows 3 clues, one at a time",
    );
  });

  it("makes one hint per builder, in order", () => {
    const hints = textHints(river, continentHint, firstLetterHint);

    expect(hints.description).toContain("2 clues");
    expect(hints.text?.(0)).toBe("Continent: Europe");
    expect(hints.text?.(1)).toBe("Starts with “D”");
    expect(hints.text?.(2)).toBeNull();
  });
});
