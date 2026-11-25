import { beforeEach, describe, expect, it } from "vitest";
import { clearDaily, resetDailyStorageForTests, writeDaily } from "./dailyStorage";
import { hasPlayedDaily, markDailyPlayed, resetDailyPlayedForTests } from "./dailyPlayed";

beforeEach(() => {
  resetDailyPlayedForTests();
  resetDailyStorageForTests();
});

describe("hasPlayedDaily", () => {
  it("is false by default", () => {
    expect(hasPlayedDaily()).toBe(false);
  });

  it("is true after markDailyPlayed", () => {
    markDailyPlayed();

    expect(hasPlayedDaily()).toBe(true);
    expect(localStorage.getItem("gg-daily-played")).toBe("1");
  });

  it("is true when only a daily entry exists, of any date", () => {
    writeDaily({ v: 1, date: "2020-01-01", number: 1, gameId: "g", difficulty: "NORMAL" });

    expect(hasPlayedDaily()).toBe(true);
  });

  it("survives clearDaily", () => {
    markDailyPlayed();
    clearDaily();

    expect(hasPlayedDaily()).toBe(true);
  });
});
