import {
  cluesDescription,
  countHint,
  firstLetterHint as firstLetter,
  type HintBuilder,
  hintsFrom,
  type HintConfig,
} from "../common/hints";
import type { River } from "./model";

export const continentHint: HintBuilder<River> = (river) => `Continent: ${river.continent}`;

/** How many countries the river flows through, without naming them. */
export const countryCountHint: HintBuilder<River> = (river) =>
  countHint(river.countries.length, "country", "countries", "Flows through");

export const firstLetterHint: HintBuilder<River> = (river) => firstLetter(river.name);

/** One hint per builder, revealed in order. */
export function textHints(river: River, ...builders: HintBuilder<River>[]): HintConfig {
  return hintsFrom(cluesDescription(builders.length), river, ...builders);
}
