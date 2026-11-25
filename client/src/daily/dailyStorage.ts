import { useSyncExternalStore } from "react";
import type { Difficulty } from "../api/types";
import { OUTCOME_ORDER } from "../games/common/outcome";
import type { RoundOutcome } from "../games/common/outcome";
import { createLocalStore } from "../settings/localStore";

const store = createLocalStore("gg-daily", "gg-daily-change");

/** Which daily challenge a game is: its date, `YYYY-MM-DD`. */
export interface DailyInfo {
  date: string;
}

export type { RoundOutcome };

/** Entries stored by an earlier version of the site used these keys; they are read as the nearest outcome. */
const LEGACY_OUTCOMES: Record<string, RoundOutcome> = {
  firstGuess: "full",
  correct: "good",
  perfect: "good",
  hints: "partial",
  revealed: "missed",
};

function currentOutcome(value: unknown): RoundOutcome | null {
  if (typeof value !== "string") return null;
  if (OUTCOME_ORDER.includes(value as RoundOutcome)) return value as RoundOutcome;
  return LEGACY_OUTCOMES[value] ?? null;
}

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
  /** The difficulty chosen for this game. */
  difficulty: Difficulty;
  /** Set once the game is finished. */
  result?: DailyResult;
}

/** `useSyncExternalStore` compares snapshots with `Object.is`, so the same string must give the same object. */
let cachedRaw: string | null = null;
let cachedEntry: DailyEntry | null = null;

function isOutcome(value: unknown): boolean {
  return currentOutcome(value) !== null;
}

function isResult(value: unknown): boolean {
  if (typeof value !== "object" || value === null) return false;
  const r = value as Record<string, unknown>;
  return (
    typeof r.score === "number" &&
    typeof r.maxScore === "number" &&
    Array.isArray(r.rounds) &&
    r.rounds.every(isOutcome)
  );
}

function isDifficulty(value: unknown): value is Difficulty {
  return value === "EASY" || value === "NORMAL" || value === "HARD";
}

/** The difficulty of an entry, or null for one saved before there was a difficulty (it is unreadable). */
function difficultyOf(e: Record<string, unknown>): Difficulty | null {
  if (isDifficulty(e.difficulty)) return e.difficulty;
  return null;
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
      (e.result !== undefined && !isResult(e.result))
    ) {
      return null;
    }
    const difficulty = difficultyOf(e);
    if (difficulty === null) return null;
    const entry: DailyEntry = { v: 1, date: e.date, number: e.number, gameId: e.gameId, difficulty };
    if (e.result !== undefined) {
      const result = e.result as DailyResult;
      entry.result = { ...result, rounds: result.rounds.map((r) => currentOutcome(r) as RoundOutcome) };
    }
    return entry;
  } catch {
    return null;
  }
}

/** The stored entry, or null if there is none or it can't be understood. */
export function readDaily(): DailyEntry | null {
  const raw = store.read();
  if (raw !== cachedRaw) {
    cachedRaw = raw;
    cachedEntry = parse(raw);
  }
  return cachedEntry;
}

/** Replaces the stored entry: only the latest daily is kept. */
export function writeDaily(entry: DailyEntry): void {
  store.write(JSON.stringify(entry));
}

export function clearDaily(): void {
  store.write(null);
}

/** The stored daily. Null on the server and during the first client render, so the static page hydrates cleanly. */
export function useDailyEntry(): DailyEntry | null {
  return useSyncExternalStore(store.subscribe, readDaily, () => null);
}

/** The entry if it was started on `date`; an entry for another day counts as none. */
export function entryFor(entry: DailyEntry | null, date: string | null): DailyEntry | null {
  return entry !== null && entry.date === date ? entry : null;
}

/** Test helper: forget the in-memory state so each test starts clean. */
export function resetDailyStorageForTests(): void {
  store.resetForTests();
  cachedRaw = null;
  cachedEntry = null;
}
