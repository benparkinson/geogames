import type { HintConfig } from "../common/MapGame";
import { firstLetterHint as firstLetter, listHint } from "../common/hints";
import type { Lake } from "./model";

export const continentHint = (lake: Lake): string => `Continent: ${lake.continent}`;

/** Names the first few countries on the shore: "Lies in" for one, "Shores in" for several. */
export const shoresHint = (lake: Lake): string | null => listHint(lake.countries.length === 1 ? "Lies in" : "Shores in", lake.countries);

export const firstLetterHint = (lake: Lake): string | null => firstLetter(lake.name);

/** The three hints of the lake game, revealed in order. */
export function lakeHints(lake: Lake): HintConfig {
  const builders = [continentHint, shoresHint, firstLetterHint];
  return {
    description: `Shows ${builders.length} clues, one at a time`,
    text: (index) => builders[index]?.(lake) ?? null,
  };
}
