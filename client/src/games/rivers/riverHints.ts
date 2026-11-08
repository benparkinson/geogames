import type { HintConfig } from "../common/MapGame";
import { firstLetterHint as firstLetter, listHint } from "../common/hints";
import type { River } from "./model";

/** Builds the text of one hint from the river. Returns null when there is nothing to say. */
export type HintBuilder = (river: River) => string | null;

export const continentHint: HintBuilder = (river) => `Continent: ${river.continent}`;

/**
 * Names the first few countries the river flows through, in the order stored (along the river),
 * and says how many more there are. A long list would give the river away.
 */
export const flowsThroughHint: HintBuilder = (river) => listHint("Flows through", river.countries);

export const firstLetterHint: HintBuilder = (river) => firstLetter(river.name);

/** One hint per builder, revealed in order. */
export function textHints(river: River, ...builders: HintBuilder[]): HintConfig {
  return {
    description: "Shows " + (builders.length === 1 ? "a clue" : `${builders.length} clues, one at a time`),
    text: (index) => builders[index]?.(river) ?? null,
  };
}
