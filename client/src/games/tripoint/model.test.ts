import { describe, expect, it } from "vitest";
import { hintedCountries } from "./model";

const order = ["A", "B", "C"];

describe("hintedCountries", () => {
  it("outlines nothing with no hints", () => {
    expect(hintedCountries(order, 0)).toEqual([]);
  });

  it("follows the hint order when nothing has been found", () => {
    expect(hintedCountries(order, 2, [[], []])).toEqual(["A", "B"]);
  });

  it("skips a country that was already found when the hint was taken", () => {
    expect(hintedCountries(order, 1, [["A"]])).toEqual(["B"]);
  });

  it("does not move an outline when its country is found later", () => {
    expect(hintedCountries(order, 2, [[], ["A"]])).toEqual(["A", "B"]);
  });

  it("falls back to a found country when every unfound one is already outlined", () => {
    expect(hintedCountries(order, 3, [[], ["A"], ["A", "B", "C"]])).toEqual(["A", "B", "C"]);
    expect(hintedCountries(["A", "B"], 2, [[], ["B"]])).toEqual(["A", "B"]);
  });

  it("treats a missing foundAtHint as nothing found", () => {
    expect(hintedCountries(order, 2)).toEqual(["A", "B"]);
  });

  it("stops when every country is outlined", () => {
    expect(hintedCountries(order, 5)).toEqual(["A", "B", "C"]);
  });
});
