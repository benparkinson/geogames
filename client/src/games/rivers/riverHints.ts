import type { HintConfig } from "../common/MapGame";
import type { River } from "./model";

/** Builds the text of one hint from the river. Returns null when there is nothing to say. */
export type HintBuilder = (river: River) => string | null;

export const continentHint: HintBuilder = (river) => `Continent: ${river.continent}`;

export const firstLetterHint: HintBuilder = (river) => {
  const letter = river.name.trim().charAt(0).toUpperCase();
  return letter ? `Starts with “${letter}”` : null;
};

/** One hint per builder, revealed in order. */
export function textHints(river: River, ...builders: HintBuilder[]): HintConfig {
  return {
    total: builders.length,
    description: "Shows " + (builders.length === 1 ? "a clue" : `${builders.length} clues, one at a time`),
    text: (index) => builders[index]?.(river) ?? null,
  };
}
