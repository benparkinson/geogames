import { useSyncExternalStore } from "react";
import { createLocalStore } from "../settings/localStore";
import { readDaily, useDailyEntry } from "./dailyStorage";

const store = createLocalStore("gg-daily-played", "gg-daily-played-change");

/** Records that this browser has started a daily. Never cleared: it says the player knows the daily. */
export function markDailyPlayed(): void {
  store.write("1");
}

/** Whether a daily has ever been started here; an existing `gg-daily` entry counts. */
export function hasPlayedDaily(): boolean {
  return store.read() === "1" || readDaily() !== null;
}

/** False on the server and during the first client render, so the static page hydrates cleanly. */
export function useHasPlayedDaily(): boolean {
  const flag = useSyncExternalStore(
    store.subscribe,
    () => store.read() === "1",
    () => false,
  );
  const entry = useDailyEntry();
  return flag || entry !== null;
}

/** Test helper: forget the in-memory state so each test starts clean. */
export function resetDailyPlayedForTests(): void {
  store.resetForTests();
}
