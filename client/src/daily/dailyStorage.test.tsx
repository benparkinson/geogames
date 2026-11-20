import { act, renderHook } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  clearDaily,
  DailyEntry,
  entryFor,
  readDaily,
  resetDailyStorageForTests,
  useDailyEntry,
  writeDaily,
} from "./dailyStorage";

const entry: DailyEntry = { v: 1, date: "2026-11-18", number: 2, gameId: "g1", difficulty: "NORMAL" };
const finished: DailyEntry = {
  ...entry,
  difficulty: "HARD",
  result: { score: 430, maxScore: 500, rounds: ["firstGuess", "hints", "correct", "missed", "firstGuess"] },
};

beforeEach(resetDailyStorageForTests);
afterEach(() => {
  vi.restoreAllMocks();
});

describe("readDaily and writeDaily", () => {
  it("round-trips an entry, with or without a result", () => {
    expect(readDaily()).toBeNull();

    writeDaily(entry);
    expect(readDaily()).toEqual(entry);

    writeDaily(finished);
    expect(readDaily()).toEqual(finished);
    expect(JSON.parse(localStorage.getItem("gg-daily")!)).toEqual(finished);
  });

  it("still loads an entry stored with the old outcome keys, mapped to the new ones", () => {
    const old = { ...entry, result: { score: 300, maxScore: 500, rounds: ["perfect", "hints", "revealed"] } };
    localStorage.setItem("gg-daily", JSON.stringify(old));

    expect(readDaily()?.result?.rounds).toEqual(["correct", "hints", "missed"]);
  });

  it("keeps only the latest entry, and clearDaily removes it", () => {
    writeDaily(entry);
    writeDaily({ ...entry, date: "2026-11-19", number: 3 });
    expect(readDaily()?.number).toBe(3);

    clearDaily();
    expect(readDaily()).toBeNull();
    expect(localStorage.getItem("gg-daily")).toBeNull();
  });

  it.each([
    ["bad JSON", "{nope"],
    ["not an object", "42"],
    ["null", "null"],
    ["the wrong version", JSON.stringify({ ...entry, v: 2 })],
    ["a missing field", JSON.stringify({ v: 1, date: "2026-11-18" })],
    ["a bad result", JSON.stringify({ ...entry, result: { score: 1, maxScore: 2, rounds: ["gold"] } })],
    ["a non-text outcome", JSON.stringify({ ...entry, result: { score: 1, maxScore: 2, rounds: [5] } })],
    ["a result that isn't an object", JSON.stringify({ ...entry, result: 5 })],
  ])("ignores %s", (_name, raw) => {
    localStorage.setItem("gg-daily", raw);

    expect(readDaily()).toBeNull();
  });

  it.each([
    [true, "HARD"],
    [false, "NORMAL"],
  ])("reads an entry saved before there was a difficulty (hardcore %s) as %s", (hardcore, difficulty) => {
    localStorage.setItem("gg-daily", JSON.stringify({ v: 1, date: "2026-11-18", number: 2, gameId: "g1", hardcore }));

    expect(readDaily()).toEqual({ v: 1, date: "2026-11-18", number: 2, gameId: "g1", difficulty });
  });

  it("prefers the difficulty of an entry to its old Hard mode flag, and ignores an unknown difficulty", () => {
    const base = { v: 1, date: "2026-11-18", number: 2, gameId: "g1" };
    localStorage.setItem("gg-daily", JSON.stringify({ ...base, difficulty: "EASY", hardcore: true }));
    expect(readDaily()?.difficulty).toBe("EASY");

    resetDailyStorageForTests();
    localStorage.setItem("gg-daily", JSON.stringify({ ...base, difficulty: "EXTREME" }));
    expect(readDaily()).toBeNull();
  });

  it("gives the same object while the stored string is unchanged", () => {
    writeDaily(entry);

    expect(readDaily()).toBe(readDaily());
  });

  it("treats an entry for another date as none", () => {
    expect(entryFor(entry, "2026-11-18")).toBe(entry);
    expect(entryFor(entry, "2026-11-19")).toBeNull();
    expect(entryFor(entry, null)).toBeNull();
    expect(entryFor(null, "2026-11-18")).toBeNull();
  });
});

describe("with localStorage blocked", () => {
  it("still holds the entry for the page", () => {
    vi.spyOn(Storage.prototype, "getItem").mockImplementation(() => {
      throw new Error("blocked");
    });
    vi.spyOn(Storage.prototype, "setItem").mockImplementation(() => {
      throw new Error("blocked");
    });

    expect(readDaily()).toBeNull();
    writeDaily(entry);
    expect(readDaily()).toEqual(entry);
    clearDaily();
    expect(readDaily()).toBeNull();
  });

  it("keeps the entry in memory when only writing fails", () => {
    vi.spyOn(Storage.prototype, "setItem").mockImplementation(() => {
      throw new Error("full");
    });

    writeDaily(entry);

    expect(readDaily()).toEqual(entry);
  });
});

describe("useDailyEntry", () => {
  it("follows this tab's writes", () => {
    const { result } = renderHook(() => useDailyEntry());
    expect(result.current).toBeNull();

    act(() => writeDaily(entry));
    expect(result.current).toEqual(entry);

    act(() => clearDaily());
    expect(result.current).toBeNull();
  });

  it("follows a change made in another tab", () => {
    const { result } = renderHook(() => useDailyEntry());

    act(() => {
      localStorage.setItem("gg-daily", JSON.stringify(entry));
      window.dispatchEvent(new StorageEvent("storage", { key: "gg-daily" }));
    });

    expect(result.current).toEqual(entry);
  });

  it("returns the same object on every render while nothing changes", () => {
    writeDaily(entry);
    const { result, rerender } = renderHook(() => useDailyEntry());
    const first = result.current;

    rerender();

    expect(result.current).toBe(first);
  });
});
