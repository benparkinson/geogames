import { SITE_NAME, SITE_URL } from "../siteName";
import { OUTCOME_STYLES } from "../games/common/outcome";
import type { DailyResult } from "../daily/dailyStorage";

/**
 * The spoiler-free text for challenging a friend: the score, one emoji per round and the link. No answers and no
 * game types.
 */
export function challengeShareText(result: DailyResult, challengeId: string): string {
  return [
    `I just played this round of ${SITE_NAME}, can you beat my score?`,
    `${result.score} / ${result.maxScore}`,
    emojiRow(result),
    challengeLink(challengeId),
  ].join("\n");
}

/** What a challenger sends back to whoever sent them the link: their score and place, spoiler-free. */
export function challengeReplyText(result: DailyResult, rank: number, entryCount: number, challengeId: string): string {
  return [
    `I took your ${SITE_NAME} challenge: ${result.score} / ${result.maxScore}, ${ordinal(rank)} of ${entryCount}.`,
    emojiRow(result),
    challengeLink(challengeId),
  ].join("\n");
}

/** What a challenger sends to someone new. `isTop` is whether theirs is the first entry in the table. */
export function challengePassOnText(
  result: DailyResult,
  topScore: number,
  isTop: boolean,
  challengeId: string,
): string {
  const invitation = isTop
    ? `I top the table with ${topScore} / ${result.maxScore}, can you beat it?`
    : `I scored ${result.score} / ${result.maxScore} on this ${SITE_NAME} challenge. ` +
      `The top score is ${topScore} / ${result.maxScore}, can you beat it?`;
  return [invitation, challengeLink(challengeId)].join("\n");
}

function emojiRow(result: DailyResult): string {
  return result.rounds.map((r) => OUTCOME_STYLES[r].emoji).join("");
}

function challengeLink(challengeId: string): string {
  return `${SITE_URL}/challenge?id=${challengeId}`;
}

/** 1st, 2nd, 3rd, 4th ... with 11th, 12th and 13th. */
function ordinal(n: number): string {
  const teen = n % 100 >= 11 && n % 100 <= 13;
  const suffix = teen ? "th" : (["th", "st", "nd", "rd"][n % 10] ?? "th");
  return `${n}${suffix}`;
}
