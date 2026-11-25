import { AnswerState, CORRECT_ANSWER } from "./Model";

/** The same formula as the backend's model/Scoring.java. Both tests hold the same table. */
export const ANSWER_POINTS = 4;
export const NO_HINT_POINTS = 3;
export const NO_MISS_POINTS = 3;
export const MAX_ROUND_POINTS = ANSWER_POINTS + NO_HINT_POINTS + NO_MISS_POINTS;

/**
 * The points a round earns, out of 10: 4 for the answer, 3 for no hints (1 off per hint) and 3 for
 * no misses (1 off per wrong guess). A correct round always earns at least the answer's 4. A
 * revealed round (gave up, or out of guesses) earns the share of the answer and no-hint points
 * that was found, `answersFound` of `answerCount` answers, rounded half up, and nothing for no
 * misses. An unanswered round earns 0.
 */
export function roundPoints(
  answerState: AnswerState,
  hintsUsed: number,
  answersFound = 0,
  answerCount = 1,
  wrongGuesses = 0,
): number {
  if (answerState === "UNANSWERED") {
    return 0;
  }
  const noHints = Math.max(0, NO_HINT_POINTS - hintsUsed);
  if (answerState === CORRECT_ANSWER) {
    return ANSWER_POINTS + noHints + Math.max(0, NO_MISS_POINTS - wrongGuesses);
  }
  const full = ANSWER_POINTS + noHints;
  // round_half_up(full * answersFound / answerCount), in integers
  return Math.floor((2 * full * answersFound + answerCount) / (2 * answerCount));
}
