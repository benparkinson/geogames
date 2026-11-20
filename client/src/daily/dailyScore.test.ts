import { describe, expect, it } from "vitest";
import { scoreBucket, topPercent } from "./dailyScore";

describe("scoreBucket", () => {
  it.each([
    [0, 0],
    [49, 0],
    [50, 1],
    [499, 9],
    [500, 10],
  ])("puts %i out of 500 in bucket %i", (score, bucket) => {
    expect(scoreBucket(score, 500)).toBe(bucket);
  });
});

describe("topPercent", () => {
  it("is the share in the player's band or higher, rounded up", () => {
    const buckets = [0, 0, 0, 1, 0, 2, 0, 0, 0, 0, 1];
    expect(topPercent(buckets, 10, 4)).toBe(25);
    expect(topPercent(buckets, 5, 4)).toBe(75);
    expect(topPercent(buckets, 0, 4)).toBe(100);
    // 1 of 3 players is 33.3%, rounded up.
    expect(topPercent([1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1], 10, 3)).toBe(34);
  });

  it("is 100 when everyone is in one band", () => {
    expect(topPercent([0, 0, 0, 0, 0, 3, 0, 0, 0, 0, 0], 5, 3)).toBe(100);
  });

  it("stays between 1 and 100 for inconsistent counts", () => {
    expect(topPercent([0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0], 3, 3)).toBe(1);
    expect(topPercent([9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9], 0, 3)).toBe(100);
  });
});
