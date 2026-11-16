import type { Coordinate, Country } from "../common/country";

export interface TripointModel {
  coordinate: Coordinate;
  countries: Country[];
}

/** The tripoint plus the order in which hints reveal its countries (by name). */
export interface TripointRoundData extends TripointModel {
  hintOrder: string[];
}

/** The countries the hints outline, in the order they were taken. */
export function hintedCountries(hintOrder: string[], hintsUsed: number, foundAtHint: string[][] = []): string[] {
  const picked: string[] = [];
  for (let i = 0; i < hintsUsed; i++) {
    const found = foundAtHint[i] ?? [];
    const next =
      hintOrder.find(name => !picked.includes(name) && !found.includes(name)) ??
      hintOrder.find(name => !picked.includes(name));
    if (next === undefined) {
      break;
    }
    picked.push(next);
  }
  return picked;
}
