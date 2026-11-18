import { useSyncExternalStore } from "react";
import { createLocalStore } from "./localStore";

// Stored name kept from when this was called Hardcore mode.
const store = createLocalStore("gg-hardcore", "gg-hard-mode-change");

function read(): boolean {
  return store.read() === "on";
}

function write(on: boolean): void {
  store.write(on ? "on" : null);
}

/**
 * The Hard mode preference for the next game: no distance or direction clues after a wrong guess. It
 * only hides the clues, so scoring is unaffected. Off by default, and kept in this browser's localStorage.
 * It is read when a game starts; each game then keeps its own setting on the server (`GameRound.hardMode`).
 */
export function useHardMode(): [boolean, (on: boolean) => void] {
  const hardMode = useSyncExternalStore(store.subscribe, read, () => false);
  return [hardMode, write];
}

/** Test helper: forget the in-memory state so each test starts clean. */
export function resetHardModeForTests(): void {
  store.resetForTests();
}
