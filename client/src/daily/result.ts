import type { GameRoundView } from "../api/types";
import { isPerfect } from "../games/common/scoring";
import type { DailyResult, RoundOutcome } from "./dailyStorage";

/** How each round of a finished game ended: perfect, some points but not perfect, or no points. */
export function resultFromGame(game: GameRoundView["gameResult"]): DailyResult {
  const rounds: RoundOutcome[] = game.roundAnswerStates.map((state, i) =>
    isPerfect(state, game.roundHintsUsed[i] ?? 0) ? "perfect" : (game.roundPoints[i] ?? 0) > 0 ? "hints" : "revealed",
  );
  return { score: game.score, maxScore: game.maxScore, rounds };
}
