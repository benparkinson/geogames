import { useSyncExternalStore } from "react";

const KEY = "gg-daily";
const CHANGE_EVENT = "gg-daily-change";

/** Which daily challenge a game is: its number and its date, `YYYY-MM-DD`. */
export interface DailyInfo {
  number: number;
  date: string;
}

/** How one round ended, as the share text shows it. */
export type RoundOutcome = "perfect" | "hints" | "revealed";

export interface DailyResult {
  score: number;
  maxScore: number;
  rounds: RoundOutcome[];
}

/** Today's daily, kept in this browser so coming back to /daily resumes it or shows the result. */
export interface DailyEntry {
  v: 1;
  /** The player's local date it was started on. */
  date: string;
  number: number;
  gameId: string;
  /** Hard mode was on for the whole game so far. */
  // Stored name kept from when this was called Hardcore mode.
  hardcore: boolean;
  /** Set once the game is finished. */
  result?: DailyResult;
}

/** Used only when localStorage is blocked: the entry then lasts until the page is reloaded. */
let fallbackRaw: string | null = null;
let useFallback = false;

/** `useSyncExternalStore` compares snapshots with `Object.is`, so the same string must give the same object. */
let cachedRaw: string | null = null;
let cachedEntry: DailyEntry | null = null;

function isOutcome(value: unknown): value is RoundOutcome {
  return value === "perfect" || value === "hints" || value === "revealed";
}

function isResult(value: unknown): value is DailyResult {
  if (typeof value !== "object" || value === null) return false;
  const r = value as Record<string, unknown>;
  return (
    typeof r.score === "number" &&
    typeof r.maxScore === "number" &&
    Array.isArray(r.rounds) &&
    r.rounds.every(isOutcome)
  );
}

function parse(raw: string | null): DailyEntry | null {
  if (raw === null) return null;
  try {
    const value: unknown = JSON.parse(raw);
    if (typeof value !== "object" || value === null) return null;
    const e = value as Record<string, unknown>;
    if (
      e.v !== 1 ||
      typeof e.date !== "string" ||
      typeof e.number !== "number" ||
      typeof e.gameId !== "string" ||
      typeof e.hardcore !== "boolean" ||
      (e.result !== undefined && !isResult(e.result))
    ) {
      return null;
    }
    return value as DailyEntry;
  } catch {
    return null;
  }
}

function readRaw(): string | null {
  if (useFallback) return fallbackRaw;
  try {
    return localStorage.getItem(KEY);
  } catch {
    useFallback = true;
    return fallbackRaw;
  }
}

/** The stored entry, or null if there is none or it can't be understood. */
export function readDaily(): DailyEntry | null {
  const raw = readRaw();
  if (raw !== cachedRaw) {
    cachedRaw = raw;
    cachedEntry = parse(raw);
  }
  return cachedEntry;
}

function writeRaw(raw: string | null): void {
  fallbackRaw = raw;
  if (!useFallback) {
    try {
      if (raw === null) {
        localStorage.removeItem(KEY);
      } else {
        localStorage.setItem(KEY, raw);
      }
    } catch {
      // Storage blocked or full: the in-memory copy holds the entry for this page.
      useFallback = true;
    }
  }
  window.dispatchEvent(new Event(CHANGE_EVENT));
}

/** Replaces the stored entry: only the latest daily is kept. */
export function writeDaily(entry: DailyEntry): void {
  writeRaw(JSON.stringify(entry));
}

export function clearDaily(): void {
  writeRaw(null);
}

function subscribe(onChange: () => void): () => void {
  window.addEventListener("storage", onChange);
  window.addEventListener(CHANGE_EVENT, onChange);
  return () => {
    window.removeEventListener("storage", onChange);
    window.removeEventListener(CHANGE_EVENT, onChange);
  };
}

/** The stored daily. Null on the server and during the first client render, so the static page hydrates cleanly. */
export function useDailyEntry(): DailyEntry | null {
  return useSyncExternalStore(subscribe, readDaily, () => null);
}

/** The entry if it was started on `date`; an entry for another day counts as none. */
export function entryFor(entry: DailyEntry | null, date: string | null): DailyEntry | null {
  return entry !== null && entry.date === date ? entry : null;
}

/** Test helper: forget the in-memory state so each test starts clean. */
export function resetDailyStorageForTests(): void {
  fallbackRaw = null;
  useFallback = false;
  cachedRaw = null;
  cachedEntry = null;
}
