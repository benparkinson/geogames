import { useSyncExternalStore } from "react";
import { createLocalStore } from "../settings/localStore";

const store = createLocalStore("gg-challenges", "gg-challenges-change");

/** How many friends' scores are kept: the games themselves are deleted after a day. */
const MAX_ENTRIES = 20;

/** The score to beat in a game that was started from a friend's challenge. */
export interface ChallengeScore {
  gameId: string;
  score: number;
  maxScore: number;
}

let cachedRaw: string | null = null;
let cachedEntries: ChallengeScore[] = [];

function parse(raw: string | null): ChallengeScore[] {
  if (raw === null) return [];
  try {
    const value: unknown = JSON.parse(raw);
    if (!Array.isArray(value)) return [];
    return value.filter(
      (e): e is ChallengeScore =>
        typeof e === "object" &&
        e !== null &&
        typeof e.gameId === "string" &&
        typeof e.score === "number" &&
        typeof e.maxScore === "number",
    );
  } catch {
    return [];
  }
}

/** `useSyncExternalStore` compares snapshots with `Object.is`, so the same string must give the same array. */
function read(): ChallengeScore[] {
  const raw = store.read();
  if (raw !== cachedRaw) {
    cachedRaw = raw;
    cachedEntries = parse(raw);
  }
  return cachedEntries;
}

/** Remembers the score to beat in `gameId`, keeping the latest `MAX_ENTRIES` entries. */
export function saveChallengeScore(entry: ChallengeScore): void {
  const others = read().filter((e) => e.gameId !== entry.gameId);
  store.write(JSON.stringify([...others, entry].slice(-MAX_ENTRIES)));
}

/** The score to beat in this game, or null if it didn't start from a challenge. Null on the server too. */
export function useChallengeScore(gameId: string): ChallengeScore | null {
  const entries = useSyncExternalStore(store.subscribe, read, () => []);
  return entries.find((e) => e.gameId === gameId) ?? null;
}

/** Test helper: forget the in-memory state so each test starts clean. */
export function resetChallengeStorageForTests(): void {
  store.resetForTests();
  cachedRaw = null;
  cachedEntries = [];
}
