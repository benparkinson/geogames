import { describe, expect, it } from "vitest";
import { endFlagIcon, startFlagIcon } from "./flagIcons";

describe("flag icons", () => {
  it("draws a green start flag and a red end flag anchored at the foot of the pole", () => {
    expect(startFlagIcon.options.html).toContain("#1e9e4a");
    expect(endFlagIcon.options.html).toContain("#d62828");
    for (const icon of [startFlagIcon, endFlagIcon]) {
      expect(icon.options.className).toBe("map-flag");
      expect(icon.options.iconAnchor).toEqual([6, 30]);
    }
  });
});
