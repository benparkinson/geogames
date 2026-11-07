import { normaliseString } from "../../helper/stringHelper";

/** Something a player can name: a canonical name plus the aliases that also count. */
export interface AnswerCandidate {
  name: string;
  additionalNames: string[];
}

/**
 * The canonical name of the candidate this guess matches, by name or alias, ignoring case,
 * accents and punctuation. Null when the guess matches none of them.
 */
export function matchAnswer(guess: string, candidates: AnswerCandidate[]): string | null {
  const normalisedGuess = normaliseString(guess);
  for (const candidate of candidates) {
    if (normaliseString(candidate.name) === normalisedGuess) {
      return candidate.name;
    }
    if (candidate.additionalNames.some(alias => normaliseString(alias) === normalisedGuess)) {
      return candidate.name;
    }
  }
  return null;
}
