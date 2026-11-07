import { afterEach, describe, expect, it, vi } from "vitest";
import { shuffle } from "./shuffle";

afterEach(() => {
  vi.restoreAllMocks();
});

describe("shuffle", () => {
  it("returns a new array with the same items and leaves the input alone", () => {
    const input = [1, 2, 3, 4, 5];

    const result = shuffle(input);

    expect(result).not.toBe(input);
    expect([...result].sort()).toEqual([1, 2, 3, 4, 5]);
    expect(input).toEqual([1, 2, 3, 4, 5]);
  });

  it("keeps the order when the random source is always 0", () => {
    vi.spyOn(Math, "random").mockReturnValue(0);

    expect(shuffle(["a", "b", "c"])).toEqual(["a", "b", "c"]);
  });

  it("can move items when the random source is high", () => {
    vi.spyOn(Math, "random").mockReturnValue(0.99);

    expect(shuffle(["a", "b", "c"])).toEqual(["c", "a", "b"]);
  });

  it("handles empty and single-item lists", () => {
    expect(shuffle([])).toEqual([]);
    expect(shuffle(["a"])).toEqual(["a"]);
  });
});
