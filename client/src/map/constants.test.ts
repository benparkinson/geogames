import { describe, expect, it } from "vitest";
import {
  answerPathOptions,
  capitalDotOptions,
  CITY_MAX_RADIUS,
  CITY_MIN_RADIUS,
  cityDotOptions,
  cityRadius,
  clueLineOptions,
  clueOutlineOptions,
  hiddenBorderOptions,
  hintLineOptions,
  lakeFillOptions,
  mergedShapeOptions,
  minorRiverOptions,
} from "./constants";

describe("map styles", () => {
  it("colours an answer differently once the player gave up", () => {
    expect(answerPathOptions(true).color).toBeTruthy();
    expect(answerPathOptions(true).color).not.toBe(answerPathOptions(false).color);
  });

  it("styles a clue differently from either answer colour", () => {
    const answers = [answerPathOptions(true).color, answerPathOptions(false).color];

    expect(answers).not.toContain(clueOutlineOptions().color);
    expect(answers).not.toContain(clueLineOptions().color);
  });

  it("fills the clue outline but not the clue lines", () => {
    expect(clueOutlineOptions().fillOpacity).toBeGreaterThan(0);
    expect(clueLineOptions().fill).toBe(false);
  });

  it("draws a hint line dashed and fainter than the clue line", () => {
    expect(clueLineOptions().dashArray).toBeUndefined();
    expect(hintLineOptions().dashArray).toBeTruthy();
    expect(hintLineOptions().opacity).toBeLessThan(1);
  });

  it("fills the merged shape opaquely with a stroke of the same colour, so no seam shows between two countries", () => {
    const merged = mergedShapeOptions();

    expect(merged.fillOpacity).toBe(1);
    expect(merged.opacity).toBe(1);
    expect(merged.color).toBe(merged.fillColor);
  });

  it("draws the hidden border in a colour that differs from the merged shape and fills nothing", () => {
    expect(hiddenBorderOptions().color).not.toBe(mergedShapeOptions().color);
    expect(hiddenBorderOptions().fill).toBe(false);
  });

  describe("cityRadius", () => {
    it("gives the largest city the largest dot and a tiny one the smallest", () => {
      expect(cityRadius(1000, 1000)).toBe(CITY_MAX_RADIUS);
      expect(cityRadius(0, 1000)).toBe(CITY_MIN_RADIUS);
      expect(cityRadius(1, 1_000_000_000)).toBeCloseTo(CITY_MIN_RADIUS, 1);
    });

    it("makes the area, not the radius, follow the population", () => {
      const quarter = cityRadius(250, 1000);

      expect(quarter).toBeCloseTo(CITY_MIN_RADIUS + (CITY_MAX_RADIUS - CITY_MIN_RADIUS) / 2);
    });

    it("never goes past the largest size, even for a population above the largest", () => {
      expect(cityRadius(5000, 1000)).toBe(CITY_MAX_RADIUS);
    });

    it("gives every city the smallest dot when no population is known", () => {
      expect(cityRadius(500, 0)).toBe(CITY_MIN_RADIUS);
    });
  });

  it("highlights a capital in a different, opaque style", () => {
    expect(capitalDotOptions().fillColor).not.toBe(cityDotOptions().fillColor);
    expect(capitalDotOptions().fillOpacity).toBe(1);
    expect(cityDotOptions().fillOpacity).toBeLessThan(1);
  });

  it("draws the minor rivers thinner than the clue line, and fills a lake", () => {
    expect(minorRiverOptions().weight).toBeLessThan(clueLineOptions().weight ?? 0);
    expect(minorRiverOptions().fill).toBe(false);
    expect(lakeFillOptions().fillOpacity).toBeGreaterThan(clueOutlineOptions().fillOpacity ?? 0);
  });
});
