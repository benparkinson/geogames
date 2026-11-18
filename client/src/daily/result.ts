import type { GameRoundView } from "../api/types";
import { isRevealed } from "../games/common/Model";
import { isPerfect } from "../games/common/scoring";
import type { DailyResult, RoundOutcome } from "./dailyStorage";

/** How each round of a finished game ended: perfect, correct with hints, or revealed. */
export function resultFromGame(game: GameRoundView["gameResult"]): DailyResult {
  const rounds: RoundOutcome[] = game.roundAnswerStates.map((state, i) =>
    isPerfect(state, game.roundHintsUsed[i] ?? 0) ? "perfect" : isRevealed(state) ? "revealed" : "hints",
  );
  return { score: game.score, maxScore: game.maxScore, rounds };
}
