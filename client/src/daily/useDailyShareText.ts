import { GameRoundView } from "../api/types";
import { FINISHED } from "../games/common/Model";
import { DailyInfo, useDailyEntry } from "./dailyStorage";
import { resultFromGame } from "./result";
import { shareText } from "./shareText";

/**
 * The spoiler-free text to share for a finished daily challenge, or null for any other game, or while the result
 * isn't known yet. Hard mode counts only if the stored entry for this day says it stayed on.
 */
export function useDailyShareText(
  daily: DailyInfo | undefined,
  gameResult: GameRoundView["gameResult"] | undefined,
): string | null {
  const dailyEntry = useDailyEntry();
  if (!daily || gameResult?.gameState !== FINISHED) {
    return null;
  }
  return shareText(
    {
      date: daily.date,
      hardMode: dailyEntry?.date === daily.date && dailyEntry.hardcore,
      result: resultFromGame(gameResult),
    },
    window.location.origin,
  );
}
