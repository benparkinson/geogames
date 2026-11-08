import type { HintConfig } from "../common/MapGame";
import type { River } from "./model";

/** Builds the text of one hint from the river. Returns null when there is nothing to say. */
export type HintBuilder = (river: River) => string | null;

export const continentHint: HintBuilder = (river) => `Continent: ${river.continent}`;

/** Names the countries the river flows through, or nothing when there are none to name. */
export const flowsThroughHint: HintBuilder = (river) =>
  river.countries.length === 0 ? null : `Flows through: ${river.countries.join(", ")}`;

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
