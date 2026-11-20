/** The comparison is shown only from this many finished games. */
export const MIN_PLAYERS = 3;

/** Must stay in step with DailyScoreStore.bucket on the backend: tenths of maxScore, 10 = a perfect score. */
export function scoreBucket(score: number, maxScore: number): number {
  return Math.min(10, Math.floor((score * 10) / maxScore));
}

/**
 * The share of players who scored in the player's band or higher, rounded up. Approximate: it works at
 * bucket level, not an exact rank. Always between 1 and 100, even if the counts are inconsistent.
 */
export function topPercent(buckets: number[], bucket: number, players: number): number {
  const atOrAbove = buckets.slice(bucket).reduce((a, b) => a + b, 0);
  return Math.min(100, Math.max(1, Math.ceil((100 * atOrAbove) / players)));
}
