import { AnswerState, CORRECT_ANSWER } from "./Model";

/** The same formula as the backend's model/Scoring.java. Both tests hold the same table. */
export const MAX_ROUND_POINTS = 100;
export const MAX_HINT_PENALTY = 50;

/**
 * The points a round earns: a correct round earns 100 less the hint penalty. A revealed round
 * (gave up, or out of guesses) earns the share of that which was found, `answersFound` of
 * `answerCount` answers, rounded half up; with none found that is 0. An unanswered round earns 0.
 */
export function roundPoints(
  answerState: AnswerState,
  hintsUsed: number,
  hintCount: number,
  answersFound = 0,
  answerCount = 1,
): number {
  if (answerState === "UNANSWERED") {
    return 0;
  }
  const full = fullPoints(hintsUsed, hintCount);
  if (answerState === CORRECT_ANSWER) {
    return full;
  }
  // round_half_up(full * answersFound / answerCount), in integers
  return Math.floor((2 * full * answersFound + answerCount) / (2 * answerCount));
}

function fullPoints(hintsUsed: number, hintCount: number): number {
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
