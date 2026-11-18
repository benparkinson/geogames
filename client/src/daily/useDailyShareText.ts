import { GameRoundView } from "../api/types";
import { FINISHED } from "../games/common/Model";
import { DailyInfo } from "./dailyStorage";
import { resultFromGame } from "./result";
import { shareText } from "./shareText";

/**
 * The spoiler-free text to share for a finished daily challenge, or null for any other game, or while the result
 * isn't known yet. Hard mode is the game's own setting.
 */
export function useDailyShareText(daily: DailyInfo | undefined, round: GameRoundView | undefined): string | null {
  if (!daily || round?.gameResult.gameState !== FINISHED) {
    return null;
  }
  return shareText(
    {
      date: daily.date,
      hardMode: round.hardMode,
      result: resultFromGame(round.gameResult),
    },
    window.location.origin,
  );
}
