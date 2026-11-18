import type { HintConfig } from "../common/hints";
import { firstLetter } from "../common/hints";
import type { CountryPairRound } from "./model";

/** Builds the text of one hint from the round. Returns null when there is nothing to say. */
export type PairHintBuilder = (round: CountryPairRound) => string | null;

/** Both countries' first letters, in name order. */
export const bothFirstLettersHint: PairHintBuilder = ({ countries }) => {
  const letters = countries.map((country) => firstLetter(country.name)?.letter);
  const [first, second] = letters;
  return first && second ? `Their names start with “${first}” and “${second}”` : null;
};

/** A hint whose text is always the same, for hints that change the map. */
export function mapHint(text: string): PairHintBuilder {
  return () => text;
}

/** One hint per builder, revealed in order. */
export function pairHints(description: string, round: CountryPairRound, ...builders: PairHintBuilder[]): HintConfig {
  return {
    description,
    text: (index) => builders[index]?.(round) ?? null,
  };
}
