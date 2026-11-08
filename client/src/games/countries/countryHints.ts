import type { HintConfig } from "../common/MapGame";
import { countHint, firstLetterHint as firstLetter, listHint } from "../common/hints";
import type { CountryShapeRound } from "./model";

/** Builds the text of one hint from the round. Returns null when there is nothing to say. */
export type HintBuilder = (round: CountryShapeRound) => string | null;

/** How many countries it borders, counting overseas territories' neighbours too. */
export const neighbourCountHint: HintBuilder = ({ country }) =>
  countHint(country.neighbours.length, "country", "countries", "Borders");

/** Names the mainland neighbours with the longest borders, at most three. */
export const neighbourNamesHint: HintBuilder = ({ country }) =>
  listHint(
    "Borders",
    country.neighbours.filter((n) => n.mainland).map((n) => n.name),
  );

export const firstLetterHint: HintBuilder = ({ country }) => firstLetter(country.name);

/** A hint whose text is always the same, for hints that change the map. */
export function mapHint(text: string): HintBuilder {
  return () => text;
}

/** One hint per builder, revealed in order. */
export function countryHints(description: string, round: CountryShapeRound, ...builders: HintBuilder[]): HintConfig {
  return {
    description,
    text: (index) => builders[index]?.(round) ?? null,
  };
}
