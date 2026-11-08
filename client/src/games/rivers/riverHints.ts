import type { HintConfig } from "../common/MapGame";
import type { River } from "./model";

/** Builds the text of one hint from the river. Returns null when there is nothing to say. */
export type HintBuilder = (river: River) => string | null;

export const continentHint: HintBuilder = (river) => `Continent: ${river.continent}`;

/** The most countries the "flows through" hint names. A long list would give the river away. */
const MAX_NAMED_COUNTRIES = 3;

/**
 * Names the first few countries the river flows through, in the order stored (along the river),
 * and says how many more there are. Gives nothing when there are none to name.
 */
export const flowsThroughHint: HintBuilder = (river) => {
  const countries = river.countries;
  if (countries.length === 0) {
    return null;
  }
  const named = countries.slice(0, MAX_NAMED_COUNTRIES);
  const more = countries.length - named.length;
  if (more === 0) {
    return `Flows through: ${named.join(", ")}`;
  }
  return `Flows through: ${named.join(", ")} and ${more} more`;
};

export const firstLetterHint: HintBuilder = (river) => {
  const letter = river.name.trim().charAt(0).toUpperCase();
  return letter ? `Starts with “${letter}”` : null;
};

/** One hint per builder, revealed in order. */
export function textHints(river: River, ...builders: HintBuilder[]): HintConfig {
  return {
    description: "Shows " + (builders.length === 1 ? "a clue" : `${builders.length} clues, one at a time`),
    text: (index) => builders[index]?.(river) ?? null,
  };
}
