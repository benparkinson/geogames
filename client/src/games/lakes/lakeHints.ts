import type { HintConfig } from "../common/hints";
import { countHint, firstLetterHint as firstLetter } from "../common/hints";
import type { Lake } from "./model";

export const continentHint = (lake: Lake): string => `Continent: ${lake.continent}`;

/** How many countries are on the shore: "Lies in" for one, "Shores in" for several. */
export const countryCountHint = (lake: Lake): string => {
  const n = lake.countries.length;
  return countHint(n, "country", "countries", n === 1 ? "Lies in" : "Shores in");
};

export const firstLetterHint = (lake: Lake): string | null => firstLetter(lake.name);

/** The three hints of the lake game, revealed in order. */
export function lakeHints(lake: Lake): HintConfig {
  const builders = [continentHint, countryCountHint, firstLetterHint];
  return {
    description: `Shows ${builders.length} clues, one at a time`,
    text: (index) => builders[index]?.(lake) ?? null,
  };
}
