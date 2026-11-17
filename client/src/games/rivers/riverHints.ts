import type { HintConfig } from "../common/MapGame";
import { countHint, firstLetterHint as firstLetter } from "../common/hints";
import type { River } from "./model";

/** Builds the text of one hint from the river. Returns null when there is nothing to say. */
export type HintBuilder = (river: River) => string | null;

export const continentHint: HintBuilder = (river) => `Continent: ${river.continent}`;

/** How many countries the river flows through, without naming them. */
export const countryCountHint: HintBuilder = (river) =>
  countHint(river.countries.length, "country", "countries", "Flows through");

export const firstLetterHint: HintBuilder = (river) => firstLetter(river.name);

/** One hint per builder, revealed in order. */
export function textHints(river: River, ...builders: HintBuilder[]): HintConfig {
  return {
    description: "Shows " + (builders.length === 1 ? "a clue" : `${builders.length} clues, one at a time`),
    text: (index) => builders[index]?.(river) ?? null,
  };
}
