import { describe, expect, it } from "vitest";
import { countHint, firstLetterHint, listHint } from "./hints";

describe("firstLetterHint", () => {
  it("gives the upper-cased first letter", () => {
    expect(firstLetterHint("Danube")).toBe("Starts with “D”");
    expect(firstLetterHint("  rhine")).toBe("Starts with “R”");
  });

  it("gives nothing for a blank name", () => {
    expect(firstLetterHint(" ")).toBeNull();
  });
});

describe("listHint", () => {
  it("gives nothing for an empty list", () => {
    expect(listHint("Borders", [])).toBeNull();
  });

  it("names everything up to the maximum", () => {
    expect(listHint("Borders", ["A"])).toBe("Borders: A");
    expect(listHint("Borders", ["A", "B", "C"])).toBe("Borders: A, B, C");
  });

  it("counts the rest past the maximum", () => {
    expect(listHint("Borders", ["A", "B", "C", "D"])).toBe("Borders: A, B, C and 1 more");
    expect(listHint("Borders", ["A", "B", "C", "D", "E", "F", "G", "H", "I"])).toBe("Borders: A, B, C and 6 more");
    expect(listHint("Borders", ["A", "B", "C"], 2)).toBe("Borders: A, B and 1 more");
  });
});

describe("countHint", () => {
  it("uses the singular for one", () => {
    expect(countHint(1, "country", "countries", "Borders")).toBe("Borders 1 country");
  });

  it("uses the plural otherwise", () => {
    expect(countHint(0, "country", "countries", "Borders")).toBe("Borders 0 countries");
    expect(countHint(5, "country", "countries", "Borders")).toBe("Borders 5 countries");
  });
});
