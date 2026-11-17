import type { HintConfig } from "../common/MapGame";
import { countHint, firstLetterHint as firstLetter } from "../common/hints";
import type { CountryShapeRound } from "./model";

/** Builds the text of one hint from the round. Returns null when there is nothing to say. */
export type HintBuilder = (round: CountryShapeRound) => string | null;

/** How many countries its mainland borders. Neighbours of overseas territories aren't counted: the map shows the mainland. */
export const neighbourCountHint: HintBuilder = ({ country }) =>
  countHint(country.neighbours.filter((n) => n.mainland).length, "country", "countries", "Its mainland borders");

/** Names the mainland neighbour with the longest border (neighbours are sorted longest first). */
export const longestBorderHint: HintBuilder = ({ country }) => {
  const longest = country.neighbours.find((n) => n.mainland);
  return longest ? `Longest border: ${longest.name}` : null;
};

/** For a landlocked country, whose lack of a coastline the map already shows. */
export const borderLengthHint: HintBuilder = ({ country }) =>
  `No coastline. Its borders are ${country.landBorderKm.toLocaleString("en-GB")} km long`;

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
