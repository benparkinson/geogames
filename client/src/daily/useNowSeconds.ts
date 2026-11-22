import { useSyncExternalStore } from "react";

const listeners = new Set<() => void>();
let timer: ReturnType<typeof setTimeout> | null = null;

function tick(): void {
  listeners.forEach((listener) => listener());
  timer = setTimeout(tick, 1000 - (Date.now() % 1000));
}

function subscribe(listener: () => void): () => void {
  listeners.add(listener);
  if (timer === null) {
    timer = setTimeout(tick, 1000 - (Date.now() % 1000));
  }
  return () => {
    listeners.delete(listener);
    if (listeners.size === 0 && timer !== null) {
      clearTimeout(timer);
      timer = null;
    }
  };
}

/** Stops the shared timer and forgets every subscriber. */
export function resetClockForTests(): void {
  listeners.clear();
  if (timer !== null) {
    clearTimeout(timer);
    timer = null;
  }
}

/**
 * The current time in whole seconds, updated once a second by one shared timer, or null on the server and in
 * the first client render (the page is pre-rendered, so nothing may read the time while rendering).
 */
export function useNowSeconds(): number | null {
  return useSyncExternalStore(
    subscribe,
    () => Math.floor(Date.now() / 1000),
    () => null,
  );
}
