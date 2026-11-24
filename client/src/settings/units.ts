import { useSyncExternalStore } from "react";
import { createLocalStore } from "./localStore";

export type DistanceUnit = "km" | "mi";

const store = createLocalStore("gg-units", "gg-units-change");

function read(): DistanceUnit {
  return store.read() === "mi" ? "mi" : "km";
}

function write(unit: DistanceUnit): void {
  store.write(unit === "mi" ? "mi" : null);
}

/** The Distances setting: kilometres (the default, nothing stored) or miles. */
export function useDistanceUnit(): [DistanceUnit, (unit: DistanceUnit) => void] {
  const unit = useSyncExternalStore(store.subscribe, read, () => "km" as const);
  return [unit, write];
}

/** Test helper: forget the in-memory state so each test starts clean. */
export function resetDistanceUnitForTests(): void {
  store.resetForTests();
}
