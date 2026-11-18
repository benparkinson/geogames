import { type HintBuilder, firstLetter } from "../common/hints";
import type { CountryPairRound } from "./model";

/** Both countries' first letters, in name order. */
export const bothFirstLettersHint: HintBuilder<CountryPairRound> = ({ countries }) => {
  const letters = countries.map((country) => firstLetter(country.name)?.letter);
  const [first, second] = letters;
  return first && second ? `Their names start with “${first}” and “${second}”` : null;
};
