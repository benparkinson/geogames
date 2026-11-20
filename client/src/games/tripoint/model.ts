import type { Coordinate, Country } from "../common/country";

export interface TripointModel {
  coordinate: Coordinate;
  countries: Country[];
}

/** The tripoint plus the order in which hints reveal its countries (by name). */
export interface TripointRoundData extends TripointModel {
  hintOrder: string[];
}

/**
 * What the panel says about the country hint `hintIndex` (0-based) outlines: a missing one, or, when the
 * player had already found every other country, one they have already found.
 */
export function tripointHintText(hintOrder: string[], hintIndex: number, foundAtHint: string[][]): string | null {
  const name = hintedCountries(hintOrder, hintIndex + 1, foundAtHint)[hintIndex];
  if (name === undefined) {
    return null;
  }
  return (foundAtHint[hintIndex] ?? []).includes(name)
    ? `Outlined on the map: ${name}, which you've already found`
    : "A missing country is outlined on the map";
}

/** The countries the hints outline, in the order they were taken. */
export function hintedCountries(hintOrder: string[], hintsUsed: number, foundAtHint: string[][] = []): string[] {
  const picked: string[] = [];
  for (let i = 0; i < hintsUsed; i++) {
    const found = foundAtHint[i] ?? [];
    const next =
      hintOrder.find((name) => !picked.includes(name) && !found.includes(name)) ??
      hintOrder.find((name) => !picked.includes(name));
    if (next === undefined) {
      break;
    }
    picked.push(next);
  }
  return picked;
}
