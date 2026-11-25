import { AnswerState, UNANSWERED } from "./Model";
import { MAX_ROUND_POINTS } from "./scoring";

/**
 * How an answered round ended, by the points it earned, the same in the round tracker, the round banner, the
 * results page and the daily share text:
 * - `full`: all 10 points.
 * - `good`: 7 to 9.
 * - `partial`: 1 to 6.
 * - `missed`: no points.
 */
export type RoundOutcome = "full" | "good" | "partial" | "missed";

/** The outcomes from best to worst. */
export const OUTCOME_ORDER: RoundOutcome[] = ["full", "good", "partial", "missed"];

/** How an answered round ended; null while unanswered. */
export function roundOutcome(answerState: AnswerState, points: number): RoundOutcome | null {
  if (answerState === UNANSWERED) return null;
  if (points >= MAX_ROUND_POINTS) return "full";
  if (points >= 7) return "good";
  return points > 0 ? "partial" : "missed";
}

/** What every view shares about an outcome, so no component chooses its own. Colours come from `[data-outcome]` in the CSS. */
export interface OutcomeStyle {
  /** The daily share text square. */
  emoji: string;
}

export const OUTCOME_STYLES: Record<RoundOutcome, OutcomeStyle> = {
  full: { emoji: "🟩" },
  good: { emoji: "🟨" },
  partial: { emoji: "🟧" },
  missed: { emoji: "🟥" },
};
