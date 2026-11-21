import {
  cluesDescription,
  firstLetterHint as firstLetter,
  type HintBuilder,
  hintsFrom,
  type HintConfig,
} from "../common/hints";
import type { River } from "./model";

export const continentHint: HintBuilder<River> = (river) => `Continent: ${river.continent}`;

/** The country with the longest stretch of the river (`countries` is sorted longest first), or that it has only one. */
export const mainCountryHint: HintBuilder<River> = (river) => {
  const [main] = river.countries;
  if (!main) {
    return null;
  }
  return river.countries.length === 1 ? `Flows only through ${main}` : `Its longest stretch is in ${main}`;
};

export const firstLetterHint: HintBuilder<River> = (river) => firstLetter(river.name);

/** One hint per builder, revealed in order. */
export function textHints(river: River, ...builders: HintBuilder<River>[]): HintConfig {
  return hintsFrom(cluesDescription(builders.length), river, ...builders);
}
