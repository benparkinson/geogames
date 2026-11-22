import { AnswerState, CORRECT_ANSWER, isRevealed, UNANSWERED } from "./Model";

/**
 * How an answered round ended, the same in the round tracker, the round banner, the results page and the daily
 * share text:
 * - `firstGuess`: correct with no hints and no wrong guess.
 * - `correct`: correct with no hints, after at least one wrong guess.
 * - `hints`: some points but not full marks: correct with hints, or revealed with some answers found.
 * - `missed`: revealed (gave up, or out of guesses) with no points.
 */
export type RoundOutcome = "firstGuess" | "correct" | "hints" | "missed";

/**
 * How an answered round ended; null while unanswered. Labels only: the points come from scoring.ts.
 * Giving up is not penalised in the label: a revealed round that earned points is `hints`, not `missed`.
 */
export function roundOutcome(
  answerState: AnswerState,
  hintsUsed: number,
  firstGuess: boolean,
  points: number,
): RoundOutcome | null {
  if (answerState === UNANSWERED) return null;
  if (isRevealed(answerState)) return points > 0 ? "hints" : "missed";
  if (answerState === CORRECT_ANSWER && hintsUsed > 0) return "hints";
  return firstGuess ? "firstGuess" : "correct";
}

/** What every view shares about an outcome, so no component chooses its own. Icons stay in the components; colours come from `[data-outcome]` in the CSS. */
export interface OutcomeStyle {
  /** The results summary noun for one round and for several. */
  singular: string;
  plural: string;
  /** The daily share text square. */
  emoji: string;
}

export const OUTCOME_STYLES: Record<RoundOutcome, OutcomeStyle> = {
  firstGuess: {
    singular: "first guess",
    plural: "first guesses",
    emoji: "❇️",
  },
  correct: {
    singular: "correct",
    plural: "correct",
    emoji: "🟩",
  },
  hints: {
    singular: "with hints",
    plural: "with hints",
    emoji: "🟨",
  },
  missed: {
    singular: "missed",
    plural: "missed",
    emoji: "🟥",
  },
};

/** The outcome keys in the order the summary line lists them. */
export const OUTCOME_ORDER: RoundOutcome[] = ["firstGuess", "correct", "hints", "missed"];
