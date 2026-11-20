import type { GameRoundView } from "../api/types";
import { roundOutcome } from "../games/common/outcome";
import type { DailyResult, RoundOutcome } from "./dailyStorage";

/** How each round of a finished game ended: first guess, correct, with hints (some points) or missed. */
export function resultFromGame(game: GameRoundView["gameResult"]): DailyResult {
  const rounds: RoundOutcome[] = game.roundAnswerStates.map(
    (state, i) =>
      roundOutcome(state, game.roundHintsUsed[i] ?? 0, game.roundFirstGuess[i] ?? false, game.roundPoints[i] ?? 0) ??
      "missed",
  );
  return { score: game.score, maxScore: game.maxScore, rounds };
}
