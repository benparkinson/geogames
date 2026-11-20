import {
  cluesDescription,
  countHint,
  firstLetterHint as firstLetter,
  hintsFrom,
  type HintConfig,
} from "../common/hints";
import type { Lake } from "./model";

export const continentHint = (lake: Lake): string => `Continent: ${lake.continent}`;

/** How many countries are on the shore: "Lies in" for one, "Shores in" for several. */
export const countryCountHint = (lake: Lake): string => {
  const n = lake.countries.length;
  return countHint(n, "country", "countries", n === 1 ? "Lies in" : "Shores in");
};

export const firstLetterHint = (lake: Lake): string | null => firstLetter(lake.name);

/**
 * The hints of the lake game, revealed in order: three, or two when the round already shows the continent (Easy), so
 * the first hint doesn't repeat it.
 */
export function lakeHints(lake: Lake, continentShown = false): HintConfig {
  const builders = continentShown
    ? [countryCountHint, firstLetterHint]
    : [continentHint, countryCountHint, firstLetterHint];
  return hintsFrom(cluesDescription(builders.length), lake, ...builders);
}
