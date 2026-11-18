import { useSyncExternalStore } from "react";

// Stored name kept from when this was called Hardcore mode.
const KEY = "gg-hardcore";
const CHANGE_EVENT = "gg-hard-mode-change";

/** Used only when localStorage is blocked: the setting then lasts until the page is reloaded. */
let fallback = false;

function read(): boolean {
  try {
    return localStorage.getItem(KEY) === "on";
  } catch {
    return fallback;
  }
}

function write(on: boolean): void {
  fallback = on;
  try {
    if (on) {
      localStorage.setItem(KEY, "on");
    } else {
      localStorage.removeItem(KEY);
    }
  } catch {
    // Storage blocked: the fallback holds the value for this page.
  }
  window.dispatchEvent(new Event(CHANGE_EVENT));
}

function subscribe(onChange: () => void): () => void {
  window.addEventListener("storage", onChange);
  window.addEventListener(CHANGE_EVENT, onChange);
  return () => {
    window.removeEventListener("storage", onChange);
    window.removeEventListener(CHANGE_EVENT, onChange);
  };
}

/**
 * Hard mode: no distance or direction clues after a wrong guess. It only hides the clues, so
 * scoring and the server are unaffected. Off by default, and kept in this browser's localStorage.
 */
export function useHardMode(): [boolean, (on: boolean) => void] {
  const hardMode = useSyncExternalStore(subscribe, read, () => false);
  return [hardMode, write];
}
