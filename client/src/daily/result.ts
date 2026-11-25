import type { GameRoundView } from "../api/types";
import { CORRECT_ANSWER } from "../games/common/Model";
import { roundOutcome } from "../games/common/outcome";
import { ANSWER_POINTS, pointsBreakdown } from "../games/common/scoring";
import type { DailyResult, RoundOutcome } from "./dailyStorage";

/** How each round of a finished game ended, by its points. */
export function resultFromGame(game: GameRoundView["gameResult"]): DailyResult {
  // A finished game has no unanswered round, so every round has an outcome.
  const rounds = game.roundAnswerStates.map((state, i) => roundOutcome(state, game.roundPoints[i]) as RoundOutcome);
  // Wrong guesses aren't kept per round, but a correct round's points less its answer and hints are its Accuracy part.
  const parts = game.roundAnswerStates.map((state, i) => {
    const points = game.roundPoints[i];
    if (state === CORRECT_ANSWER) {
      const { answer, noHints } = pointsBreakdown(state, game.roundHintsUsed[i]);
      return { answer, noHints, noMisses: Math.max(0, points - answer - noHints) };
    }
    return { answer: Math.min(points, ANSWER_POINTS), noHints: Math.max(0, points - ANSWER_POINTS), noMisses: 0 };
  });
  return { score: game.score, maxScore: game.maxScore, rounds, parts };
}
