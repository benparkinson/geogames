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
 * Hard mode: no distance or direction clues after a wrong guess. It only hides the clues, so
 * scoring and the server are unaffected. Off by default, and kept in this browser's localStorage.
 */
export function useHardMode(): [boolean, (on: boolean) => void] {
  const hardMode = useSyncExternalStore(store.subscribe, read, () => false);
  return [hardMode, write];
}

/** Test helper: forget the in-memory state so each test starts clean. */
export function resetHardModeForTests(): void {
  store.resetForTests();
}
