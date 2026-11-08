import { describe, expect, it } from "vitest";
import { mergeShapes, shapeOf } from "./shape";

describe("shapeOf", () => {
  it("describes objects with sorted keys and primitives by type", () => {
    expect(JSON.stringify(shapeOf({ b: 1, a: "x", c: true, d: null }))).toBe(
      '{"a":"string","b":"number","c":"boolean","d":"null"}',
    );
  });

  it("describes arrays by their merged elements, and empty arrays as empty", () => {
    expect(shapeOf([])).toEqual({ array: "empty" });
    expect(shapeOf([{ a: 1 }, { b: "x" }, { a: 2 }])).toEqual({ array: { a: "number", b: "string" } });
  });

  it("treats the value of coordinates as opaque", () => {
    expect(shapeOf({ coordinates: [[[1, 2]]] })).toEqual({ coordinates: "coordinates" });
  });
});

describe("mergeShapes", () => {
  it("lets empty and null give way", () => {
    expect(mergeShapes("empty", "string")).toBe("string");
    expect(mergeShapes("number", "null")).toBe("number");
    expect(mergeShapes("null", { a: "string" })).toEqual({ a: "string" });
  });

  it("joins different primitives, sorted", () => {
    expect(mergeShapes("string", "number")).toBe("number|string");
    expect(mergeShapes("number|string", "boolean")).toBe("boolean|number|string");
  });

  it("refuses an object merged with a primitive", () => {
    expect(() => mergeShapes({ a: "string" }, "number")).toThrow(/Cannot merge/);
  });
});
