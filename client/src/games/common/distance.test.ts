import { describe, expect, it } from "vitest";
import { bearingDegrees, compassPoint, distanceKm, formatKm } from "./distance";

const london = { lat: 51.51, lon: -0.13 };
const paris = { lat: 48.86, lon: 2.35 };

describe("distanceKm", () => {
  it("measures London to Paris at about 344 km", () => {
    expect(distanceKm(london, paris)).toBeCloseTo(344, -1);
  });

  it("is zero for the same point", () => {
    expect(distanceKm(paris, paris)).toBe(0);
  });

  it("goes the short way across the antimeridian", () => {
    expect(distanceKm({ lat: 0, lon: 179 }, { lat: 0, lon: -179 })).toBeCloseTo(222, 0);
  });
});

describe("bearingDegrees", () => {
  it("points south-east from London to Paris", () => {
    expect(bearingDegrees(london, paris)).toBeCloseTo(148, 0);
    expect(compassPoint(bearingDegrees(london, paris))).toBe("south-east");
  });

  it("is 0 due north and 90 due east", () => {
    expect(bearingDegrees({ lat: 0, lon: 0 }, { lat: 10, lon: 0 })).toBeCloseTo(0, 5);
    expect(bearingDegrees({ lat: 0, lon: 0 }, { lat: 0, lon: 10 })).toBeCloseTo(90, 5);
  });

  it("points east across the antimeridian", () => {
    expect(bearingDegrees({ lat: 0, lon: 179 }, { lat: 0, lon: -179 })).toBeCloseTo(90, 5);
  });
});

describe("compassPoint", () => {
  it("switches sector at 22.5°", () => {
    expect(compassPoint(22.4)).toBe("north");
    expect(compassPoint(22.6)).toBe("north-east");
  });

  it("wraps round at north", () => {
    expect(compassPoint(359)).toBe("north");
    expect(compassPoint(337.6)).toBe("north");
    expect(compassPoint(337.4)).toBe("north-west");
  });

  it("names every point", () => {
    expect([0, 45, 90, 135, 180, 225, 270, 315].map(compassPoint)).toEqual([
      "north",
      "north-east",
      "east",
      "south-east",
      "south",
      "south-west",
      "west",
      "north-west",
    ]);
  });
});

describe("formatKm", () => {
  it("rounds to 10 km with a thousands separator", () => {
    expect(formatKm(1236)).toBe("1,240");
    expect(formatKm(344)).toBe("340");
    expect(formatKm(4)).toBe("0");
  });
});
