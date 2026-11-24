import { describe, expect, it } from "vitest";
import { bearingDegrees, compassPoint, distanceKm, formatDistance } from "./distance";

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
    expect(bearingDegrees(london, paris)).toBeCloseTo(149, 0);
    expect(compassPoint(bearingDegrees(london, paris))).toBe("south-east");
  });

  it("is 0 due north and 90 due east", () => {
    expect(bearingDegrees({ lat: 0, lon: 0 }, { lat: 10, lon: 0 })).toBeCloseTo(0, 5);
    expect(bearingDegrees({ lat: 0, lon: 0 }, { lat: 0, lon: 10 })).toBeCloseTo(90, 5);
  });

  it("points east and south, not north-east, from South Korea to Paraguay (the great circle goes over the pole)", () => {
    const korea = { lat: 37, lon: 127.5 };
    const paraguay = { lat: -23.4, lon: -58.4 };
    expect(distanceKm(korea, paraguay)).toBeGreaterThan(17000);
    const bearing = bearingDegrees(korea, paraguay);
    expect(bearing).toBeGreaterThan(90);
    expect(bearing).toBeLessThan(180);
    expect(compassPoint(bearing)).not.toBe("north-east");
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

describe("formatDistance", () => {
  it("rounds to 10 of the unit with a thousands separator", () => {
    expect(formatDistance(1236, "km")).toBe("1,240 km");
    expect(formatDistance(344, "km")).toBe("340 km");
    expect(formatDistance(1236, "mi")).toBe("770 mi");
    expect(formatDistance(4, "mi")).toBe("0 mi");
    expect(formatDistance(16093, "mi")).toBe("10,000 mi");
  });
});
