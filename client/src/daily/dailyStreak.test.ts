import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  currentStreak,
  nextStreak,
  parse,
  previousDay,
  readStreak,
  recordFinished,
  resetDailyStreakForTests,
} from "./dailyStreak";

beforeEach(resetDailyStreakForTests);

const streak = (lastDate: string, count: number) => ({ v: 1 as const, lastDate, count });

describe("previousDay", () => {
  it.each([
    ["2026-11-22", "2026-11-21"],
    ["2026-03-01", "2026-02-28"],
    ["2028-03-01", "2028-02-29"],
    ["2026-01-01", "2025-12-31"],
    // The days after the clocks change in Europe and the US.
    ["2026-03-30", "2026-03-29"],
    ["2026-10-26", "2026-10-25"],
    ["2026-03-09", "2026-03-08"],
    ["2026-11-02", "2026-11-01"],
  ])("the day before %s is %s", (date, expected) => {
    expect(previousDay(date)).toBe(expected);
  });
});

describe("nextStreak", () => {
  it("starts at 1 with no streak", () => {
    expect(nextStreak(null, "2026-11-22")).toEqual(streak("2026-11-22", 1));
  });

  it("does not count the same day twice", () => {
    const prev = streak("2026-11-22", 3);
    expect(nextStreak(prev, "2026-11-22")).toBe(prev);
  });

  it("adds the next day", () => {
    expect(nextStreak(streak("2026-11-21", 3), "2026-11-22")).toEqual(streak("2026-11-22", 4));
  });

  it("starts again after a gap", () => {
    expect(nextStreak(streak("2026-11-20", 3), "2026-11-22")).toEqual(streak("2026-11-22", 1));
  });

  it("starts again when the last date is in the future", () => {
    expect(nextStreak(streak("2026-11-25", 3), "2026-11-22")).toEqual(streak("2026-11-22", 1));
  });
});

describe("currentStreak", () => {
  it("shows a streak ending today", () => {
    expect(currentStreak(streak("2026-11-22", 4), "2026-11-22")).toBe(4);
  });

  it("still shows a streak ending yesterday", () => {
    expect(currentStreak(streak("2026-11-21", 4), "2026-11-22")).toBe(4);
  });

  it("shows nothing for a streak ending two days ago", () => {
    expect(currentStreak(streak("2026-11-20", 4), "2026-11-22")).toBe(0);
  });

  it("shows nothing with no streak or no date", () => {
    expect(currentStreak(null, "2026-11-22")).toBe(0);
    expect(currentStreak(streak("2026-11-22", 4), null)).toBe(0);
  });
});

describe("parse", () => {
  it("reads a valid streak", () => {
    expect(parse('{"v":1,"lastDate":"2026-11-22","count":2}')).toEqual(streak("2026-11-22", 2));
  });

  it.each([
    ["garbage", "not json"],
    ["a non-object", "3"],
    ["a wrong version", '{"v":2,"lastDate":"2026-11-22","count":2}'],
    ["a bad date", '{"v":1,"lastDate":"22/11/2026","count":2}'],
    ["a count of 0", '{"v":1,"lastDate":"2026-11-22","count":0}'],
    ["a fractional count", '{"v":1,"lastDate":"2026-11-22","count":1.5}'],
  ])("rejects %s", (_, raw) => {
    expect(parse(raw)).toBeNull();
  });
});

describe("recordFinished", () => {
  it("stores the streak", () => {
    recordFinished("2026-11-21");
    recordFinished("2026-11-22");

    expect(JSON.parse(localStorage.getItem("gg-daily-streak") ?? "null")).toEqual(streak("2026-11-22", 2));
  });

  it("keeps the streak in memory when storage is blocked", () => {
    vi.stubGlobal("localStorage", {
      getItem: () => {
        throw new Error("blocked");
      },
      setItem: () => {
        throw new Error("blocked");
      },
      removeItem: () => {
        throw new Error("blocked");
      },
    });

    recordFinished("2026-11-21");
    recordFinished("2026-11-22");

    expect(readStreak()).toEqual(streak("2026-11-22", 2));
  });
});
