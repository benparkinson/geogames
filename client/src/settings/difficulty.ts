import { useSyncExternalStore } from "react";
import type { Difficulty } from "../api/types";
import { createLocalStore } from "./localStore";

const store = createLocalStore("gg-difficulty", "gg-difficulty-change");

/** The key the Hard mode switch used before there was a Difficulty setting. */
const LEGACY_HARD_MODE_KEY = "gg-hardcore";

/** Whether the old Hard mode switch was on. Only read: nothing is rewritten while rendering. */
function legacyHardMode(): boolean {
  try {
    return localStorage.getItem(LEGACY_HARD_MODE_KEY) === "on";
  } catch {
    return false;
  }
}

function read(): Difficulty {
  const raw = store.read();
  if (raw === "easy") return "EASY";
  if (raw === "hard") return "HARD";
  // Nothing stored under the new key: an old Hard mode preference still counts.
  return raw === null && legacyHardMode() ? "HARD" : "NORMAL";
}

function write(difficulty: Difficulty): void {
  // The first write of the new key retires the old one. It goes on every write, or choosing Normal would
  // leave an old "on" to be read as Hard.
  try {
    localStorage.removeItem(LEGACY_HARD_MODE_KEY);
  } catch {
    // Storage blocked: there is no old value to retire.
  }
  store.write(difficulty === "EASY" ? "easy" : difficulty === "HARD" ? "hard" : null);
}

/**
 * The difficulty for the next game: Easy shows the continent and doubles the guesses, Normal is the game as it is,
 * Hard hides the distance and direction clues. Normal by default, and kept in this browser's localStorage. It is read
 * when a game starts; each game then keeps its own difficulty on the server (`GameRound.difficulty`).
 */
export function useDifficulty(): [Difficulty, (difficulty: Difficulty) => void] {
  const difficulty = useSyncExternalStore(store.subscribe, read, () => "NORMAL" as const);
  return [difficulty, write];
}

/** Test helper: forget the in-memory state so each test starts clean. */
export function resetDifficultyForTests(): void {
  store.resetForTests();
}
