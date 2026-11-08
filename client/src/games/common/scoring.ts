import { AnswerState, CORRECT_ANSWER } from "./Model";

/** The same formula as the backend's model/Scoring.java. Both tests hold the same table. */
export const MAX_ROUND_POINTS = 100;
export const MAX_HINT_PENALTY = 50;

/** The points a round earns: 100 if perfect, less for each hint, 0 unless correct. */
export function roundPoints(answerState: AnswerState, hintsUsed: number, hintCount: number): number {
  if (answerState !== CORRECT_ANSWER) {
    return 0;
  }
  if (hintCount === 0) {
    return MAX_ROUND_POINTS;
  }
  // round_half_up(MAX_HINT_PENALTY * hintsUsed / hintCount), in integers
  const penalty = Math.floor((2 * MAX_HINT_PENALTY * hintsUsed + hintCount) / (2 * hintCount));
  return MAX_ROUND_POINTS - penalty;
}

/** Correct without a single hint. */
export function isPerfect(answerState: AnswerState, hintsUsed: number): boolean {
  return answerState === CORRECT_ANSWER && hintsUsed === 0;
}
