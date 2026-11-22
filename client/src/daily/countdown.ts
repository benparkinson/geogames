/** Whole seconds from `now` to the next local midnight, rounded up. Local-date arithmetic, so a DST day is right. */
export function secondsToNextDaily(now: Date): number {
  const next = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1);
  return Math.ceil((next.getTime() - now.getTime()) / 1000);
}

/** `H:MM:SS`: hours not padded, so 5:12:33, 0:00:09 and (on a 25-hour DST day) 24:59:59. */
export function formatCountdown(seconds: number): string {
  const s = Math.max(0, Math.floor(seconds));
  const hours = Math.floor(s / 3600);
  const minutes = String(Math.floor((s % 3600) / 60)).padStart(2, "0");
  const secs = String(s % 60).padStart(2, "0");
  return `${hours}:${minutes}:${secs}`;
}
