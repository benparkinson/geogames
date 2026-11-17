import { describe, expect, it } from "vitest";
import { answerPathOptions, clueLineOptions, clueOutlineOptions, hiddenBorderOptions, hintLineOptions, mergedShapeOptions } from "./constants";

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
});
