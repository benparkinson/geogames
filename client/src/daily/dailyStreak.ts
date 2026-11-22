import { useSyncExternalStore } from "react";
import { localDateString } from "../api/daily";
import { createLocalStore } from "../settings/localStore";

/**
 * How many days in a row this browser has finished the daily challenge. Written only when a daily is finished,
 * keyed by the date the daily belongs to, and never sent anywhere.
 */
const store = createLocalStore("gg-daily-streak", "gg-daily-streak-change");

export interface DailyStreak {
  v: 1;
  /** The date (`YYYY-MM-DD`) of the last daily finished. */
  lastDate: string;
  /** Days in a row ending on `lastDate`, at least 1. */
  count: number;
}

const DATE = /^(\d{4})-(\d{2})-(\d{2})$/;

/** `useSyncExternalStore` compares snapshots with `Object.is`, so the same string must give the same object. */
let cachedRaw: string | null = null;
let cachedValue: DailyStreak | null = null;

export function parse(raw: string | null): DailyStreak | null {
  if (raw === null) return null;
  try {
    const value: unknown = JSON.parse(raw);
    if (typeof value !== "object" || value === null) return null;
    const s = value as Record<string, unknown>;
    if (
      s.v !== 1 ||
      typeof s.lastDate !== "string" ||
      !DATE.test(s.lastDate) ||
      typeof s.count !== "number" ||
      !Number.isInteger(s.count) ||
      s.count < 1
    ) {
      return null;
    }
    return { v: 1, lastDate: s.lastDate, count: s.count };
  } catch {
    return null;
  }
}

/** The day before `date` (`YYYY-MM-DD`), by local-date arithmetic so month ends, year ends and DST are safe. */
export function previousDay(date: string): string {
  const match = DATE.exec(date);
  if (match === null) throw new Error(`Not a date: ${date}`);
  const [, y, m, d] = match;
  return localDateString(new Date(Number(y), Number(m) - 1, Number(d) - 1));
}

/** The streak after finishing the daily of `date`. */
export function nextStreak(prev: DailyStreak | null, date: string): DailyStreak {
  if (prev !== null && prev.lastDate === date) return prev;
  if (prev !== null && prev.lastDate === previousDay(date)) return { v: 1, lastDate: date, count: prev.count + 1 };
  return { v: 1, lastDate: date, count: 1 };
}

/** The streak to show on `today`: still alive if the last daily finished was today's or yesterday's. */
export function currentStreak(stored: DailyStreak | null, today: string | null): number {
  if (stored === null || today === null) return 0;
  return stored.lastDate === today || stored.lastDate === previousDay(today) ? stored.count : 0;
}

/** The stored streak, or null if there is none or it can't be understood. */
export function readStreak(): DailyStreak | null {
  const raw = store.read();
  if (raw !== cachedRaw) {
    cachedRaw = raw;
    cachedValue = parse(raw);
  }
  return cachedValue;
}

/** Counts the daily of `date` as finished. Finishing the same day again changes nothing. */
export function recordFinished(date: string): void {
  const prev = readStreak();
  const next = nextStreak(prev, date);
  if (next !== prev) store.write(JSON.stringify(next));
}

/** The stored streak. Null on the server and during the first client render, so the static page hydrates cleanly. */
export function useDailyStreak(): DailyStreak | null {
  return useSyncExternalStore(store.subscribe, readStreak, () => null);
}

/** Test helper: forget the in-memory state so each test starts clean. */
export function resetDailyStreakForTests(): void {
  store.resetForTests();
  cachedRaw = null;
  cachedValue = null;
}
