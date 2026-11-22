import { describe, expect, it } from "vitest";
import { formatCountdown, secondsToNextDaily } from "./countdown";

describe("secondsToNextDaily", () => {
  it.each([
    ["one second before midnight", new Date(2026, 5, 10, 23, 59, 59), 1],
    ["at midnight", new Date(2026, 5, 10, 0, 0, 0), 86400],
    ["mid-day", new Date(2026, 5, 10, 12, 0, 0), 43200],
    ["at a month end", new Date(2026, 0, 31, 23, 0, 0), 3600],
    ["at a year end", new Date(2026, 11, 31, 18, 30, 0), 5.5 * 3600],
  ])("%s", (_, now, expected) => {
    expect(secondsToNextDaily(now)).toBe(expected);
  });

  it("rounds a part second up", () => {
    expect(secondsToNextDaily(new Date(2026, 5, 10, 23, 59, 59, 500))).toBe(1);
  });

  it.each([new Date(2026, 2, 29, 0, 0, 0), new Date(2026, 9, 25, 0, 0, 0), new Date(2026, 2, 8, 0, 0, 0)])(
    "uses the local next midnight on %s, whatever the zone's DST",
    (now) => {
      const next = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1);
      expect(secondsToNextDaily(now)).toBe((next.getTime() - now.getTime()) / 1000);
    },
  );
});

describe("formatCountdown", () => {
  it.each([
    [0, "0:00:00"],
    [9, "0:00:09"],
    [59, "0:00:59"],
    [3600, "1:00:00"],
    [5 * 3600 + 12 * 60 + 33, "5:12:33"],
    [24 * 3600 + 59 * 60 + 59, "24:59:59"],
  ])("%i seconds", (seconds, text) => {
    expect(formatCountdown(seconds)).toBe(text);
  });
});
