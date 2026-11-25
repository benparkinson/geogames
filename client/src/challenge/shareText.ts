import { OUTCOME_STYLES } from "../games/common/outcome";
import type { DailyResult } from "../daily/dailyStorage";

/**
 * The spoiler-free text for challenging a friend: the score, one emoji per round and the link. No answers and no
 * game types.
 */
export function challengeShareText(result: DailyResult, challengeId: string, origin: string): string {
  return [
    "I just played this round of GeoGauntlet, can you beat my score?",
    `${result.score} / ${result.maxScore}`,
    result.rounds.map((r) => OUTCOME_STYLES[r].emoji).join(""),
    `${origin}/challenge?id=${challengeId}`,
  ].join("\n");
}
