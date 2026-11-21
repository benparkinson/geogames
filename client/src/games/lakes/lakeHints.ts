import { cluesDescription, firstLetterHint as firstLetter, hintsFrom, type HintConfig } from "../common/hints";
import type { Lake } from "./model";

export const continentHint = (lake: Lake): string => `Continent: ${lake.continent}`;

/** The country with the longest shoreline (`countries` is sorted longest first), or that it lies in one country. */
export const mainCountryHint = (lake: Lake): string | null => {
  const [main] = lake.countries;
  if (!main) {
    return null;
  }
  return lake.countries.length === 1 ? `Lies entirely in ${main}` : `Its longest shoreline is in ${main}`;
};

export const firstLetterHint = (lake: Lake): string | null => firstLetter(lake.name);

/**
 * The hints of the lake game, revealed in order: three, or two when the round already shows the continent (Easy), so
 * the first hint doesn't repeat it.
 */
export function lakeHints(lake: Lake, continentShown = false): HintConfig {
  const builders = continentShown
    ? [mainCountryHint, firstLetterHint]
    : [continentHint, mainCountryHint, firstLetterHint];
  return hintsFrom(cluesDescription(builders.length), lake, ...builders);
}
