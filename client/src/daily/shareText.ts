import type { DailyResult, RoundOutcome } from "./dailyStorage";

const SQUARES: Record<RoundOutcome, string> = {
  perfect: "🟩",
  hints: "🟨",
  revealed: "🟥",
};

/**
 * The spoiler-free text a player copies: the score and one square per round, in order. No answers, game types
 * or hint counts. The link is always /daily, never the game, so nobody can open (and answer) the sharer's game.
 */
export function shareText(
  daily: { date: string; hardcore: boolean; result: DailyResult },
  origin: string,
): string {
  const title = `GeoGauntlet Daily ${daily.date}` + (daily.hardcore ? " · Hard mode" : "");
  return [
    title,
    `${daily.result.score} / ${daily.result.maxScore}`,
    daily.result.rounds.map(r => SQUARES[r]).join(""),
    `${origin}/daily`,
  ].join("\n");
}
