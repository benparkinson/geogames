import { describe, expect, it } from "vitest";
import { MAP_PALETTE } from "./palette";
import { mouthIcon, sourceIcon, targetIcon, tripointIcon } from "./markerIcons";

const all = [sourceIcon, mouthIcon, targetIcon, tripointIcon];

describe("marker icons", () => {
  it("draws a teal source pin and a red mouth pin", () => {
    expect(sourceIcon.options.html).toContain("#1d5c63");
    expect(mouthIcon.options.html).toContain("#d9481c");
  });

  it("draws every pin in a MAP_PALETTE colour", () => {
    expect(sourceIcon.options.html).toContain(MAP_PALETTE.sourcePin);
    expect(mouthIcon.options.html).toContain(MAP_PALETTE.mouthPin);
    expect(targetIcon.options.html).toContain(MAP_PALETTE.targetPin);
    expect(tripointIcon.options.html).toContain(MAP_PALETTE.tripointPin);
  });

  it("gives every marker its own glyph", () => {
    const html = all.map((icon) => String(icon.options.html));
    expect(new Set(html).size).toBe(all.length);
  });

  it("anchors every pin at its tip", () => {
    for (const icon of all) {
      expect(icon.options.className).toBe("map-marker");
      expect(icon.options.iconSize).toEqual([28, 36]);
      expect(icon.options.iconAnchor).toEqual([14, 34]);
    }
  });
});
