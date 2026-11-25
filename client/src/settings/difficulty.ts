import { useSyncExternalStore } from "react";
import type { Difficulty } from "../api/types";
import { createLocalStore } from "./localStore";

const store = createLocalStore("gg-difficulty", "gg-difficulty-change");

function read(): Difficulty {
  const raw = store.read();
  if (raw === "easy") return "EASY";
  if (raw === "hard") return "HARD";
  return "NORMAL";
}

function write(difficulty: Difficulty): void {
  store.write(difficulty === "EASY" ? "easy" : difficulty === "HARD" ? "hard" : null);
}

/**
 * The difficulty for the next game: Easy shows the continent and gives more guesses, Normal is the game as it is,
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
