import { describe, expect, it } from "vitest";
import { prepareSuggestions, rankSuggestions } from "./suggestions";

const list = prepareSuggestions([
  { name: "Côte d'Ivoire", aliases: ["Ivory Coast"] },
  { name: "Nepal", aliases: [] },
  { name: "Netherlands", aliases: ["Holland"] },
  { name: "New Zealand", aliases: ["Aotearoa"] },
  { name: "Niger", aliases: [] },
  { name: "Nigeria", aliases: [] },
  { name: "Guinea-Bissau", aliases: [] },
  { name: "Sri Lanka", aliases: ["Ceylon"] },
]);

const names = (q: string, limit?: number) => rankSuggestions(q, list, limit).map((m) => m.name);

describe("rankSuggestions", () => {
  it.each([
    ["name prefix (tier 1)", "nig", ["Niger", "Nigeria"]],
    ["later word of the name (tier 2)", "zea", ["New Zealand"]],
    ["later word after a hyphen (tier 2)", "bis", ["Guinea-Bissau"]],
    ["alias prefix (tier 3)", "hol", ["Netherlands"]],
    ["name contains (tier 4)", "land", ["Netherlands", "New Zealand"]],
    ["alias contains (tier 5)", "ylo", ["Sri Lanka"]],
  ])("%s", (_label, query, expected) => {
    expect(names(query)).toEqual(expected);
  });

  it("orders the tiers, keeping input order within one", () => {
    // Three names start with "ne"; Guinea-Bissau only contains it.
    expect(names("ne")).toEqual(["Nepal", "Netherlands", "New Zealand", "Guinea-Bissau"]);
    // "lan": "Lanka" is a later word (2) before "Netherlands" (4).
    expect(names("lan")).toEqual(["Sri Lanka", "Netherlands", "New Zealand"]);
  });

  it("still offers longer names on an exact match", () => {
    expect(names("niger")).toEqual(["Niger", "Nigeria"]);
  });

  it("limits the result", () => {
    expect(names("n", 2)).toEqual(["Nepal", "Netherlands"]);
    expect(names("n")).toHaveLength(5);
  });

  it("ignores case and accents", () => {
    expect(names("COTE")).toEqual(["Côte d'Ivoire"]);
    expect(names("côte d")).toEqual(["Côte d'Ivoire"]);
  });

  it("returns nothing for an empty or punctuation-only query", () => {
    expect(names("")).toEqual([]);
    expect(names(" - ' ")).toEqual([]);
  });

  it("reports the alias only for the alias tiers", () => {
    expect(rankSuggestions("hol", list)).toEqual([{ name: "Netherlands", alias: "Holland" }]);
    expect(rankSuggestions("ylo", list)).toEqual([{ name: "Sri Lanka", alias: "Ceylon" }]);
    expect(rankSuggestions("nepal", list)).toEqual([{ name: "Nepal" }]);
  });

  it("lists a place once when the name and an alias both match", () => {
    const both = prepareSuggestions([{ name: "Netherlands", aliases: ["Nederland", "Netherlands (the)"] }]);
    expect(rankSuggestions("ne", both)).toEqual([{ name: "Netherlands" }]);
  });
});
