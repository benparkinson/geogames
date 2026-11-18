/**
 * One string kept in `localStorage` under `key`, for `useSyncExternalStore`. If the browser blocks storage (a private
 * window, or the quota is full) the value is kept in memory instead, so the setting still holds until the page is
 * reloaded. `changeEvent` tells every hook in this tab about a write; other tabs are told by the `storage` event.
 */
export function createLocalStore(
  key: string,
  changeEvent: string,
): {
  read: () => string | null;
  write: (raw: string | null) => void;
  subscribe: (onChange: () => void) => () => void;
  resetForTests: () => void;
} {
  /** Used only when localStorage is blocked: the value then lasts until the page is reloaded. */
  let fallbackRaw: string | null = null;
  let useFallback = false;

  function read(): string | null {
    if (useFallback) return fallbackRaw;
    try {
      return localStorage.getItem(key);
    } catch {
      useFallback = true;
      return fallbackRaw;
    }
  }

  /** `null` removes the value. */
  function write(raw: string | null): void {
    fallbackRaw = raw;
    if (!useFallback) {
      try {
        if (raw === null) {
          localStorage.removeItem(key);
        } else {
          localStorage.setItem(key, raw);
        }
      } catch {
        // Storage blocked or full: the in-memory copy holds the value for this page, and from now on it is read.
        useFallback = true;
      }
    }
    window.dispatchEvent(new Event(changeEvent));
  }

  function subscribe(onChange: () => void): () => void {
    window.addEventListener("storage", onChange);
    window.addEventListener(changeEvent, onChange);
    return () => {
      window.removeEventListener("storage", onChange);
      window.removeEventListener(changeEvent, onChange);
    };
  }

  /** Test helper: forget the in-memory state so each test starts clean. */
  function resetForTests(): void {
    fallbackRaw = null;
    useFallback = false;
  }

  return { read, write, subscribe, resetForTests };
}
