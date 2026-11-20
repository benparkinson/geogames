import { OUTCOME_STYLES } from "../games/common/outcome";
import type { DailyResult } from "./dailyStorage";

/**
 * The spoiler-free text a player copies: the score and one square per round, in order. No answers, game types
 * or hint counts. The link is always /daily, never the game, so nobody can open (and answer) the sharer's game.
 */
export function shareText(daily: { date: string; hardMode: boolean; result: DailyResult }, origin: string): string {
  const title = `GeoGauntlet Daily ${daily.date}` + (daily.hardMode ? " · Hard mode" : "");
  return [
    title,
    `${daily.result.score} / ${daily.result.maxScore}`,
    daily.result.rounds.map((r) => OUTCOME_STYLES[r].emoji).join(""),
    `${origin}/daily`,
  ].join("\n");
}
