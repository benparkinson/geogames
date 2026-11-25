import { SITE_NAME, SITE_URL } from "../siteName";
import type { Difficulty } from "../api/types";
import { OUTCOME_STYLES } from "../games/common/outcome";
import type { DailyResult } from "./dailyStorage";

const TITLE_SUFFIX: Record<Difficulty, string> = { EASY: " · Easy", NORMAL: "", HARD: " · Hard mode" };

/**
 * The spoiler-free text a player copies: the score and one square per round, in order. No answers, game types
 * or hint counts. The link is always /daily, never the game, so nobody can open (and answer) the sharer's game.
 */
export function shareText(daily: { date: string; difficulty: Difficulty; result: DailyResult }): string {
  const title = `${SITE_NAME} Daily ${daily.date}` + TITLE_SUFFIX[daily.difficulty];
  return [
    title,
    `${daily.result.score} / ${daily.result.maxScore}`,
    daily.result.rounds.map((r) => OUTCOME_STYLES[r].emoji).join(""),
    `${SITE_URL}/daily`,
  ].join("\n");
}
