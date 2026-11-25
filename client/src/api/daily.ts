import { useMutation, useQuery } from "@tanstack/react-query";
import { fetchJson, hasStatus, postJson } from "./http";
import { queryKeys } from "./queryKeys";
import type { DailyChallenge, DailyGameResponse, DailyScore, Difficulty } from "./types";

/**
 * The day's totals for one difficulty league of the daily challenge. They go stale after a minute as others finish;
 * no polling.
 */
export function useDailyScore(date: string, difficulty: Difficulty) {
  return useQuery({
    queryKey: queryKeys.dailyScore(date, difficulty),
    queryFn: () => fetchJson<DailyScore>(`/api/daily-scores/${date}?difficulty=${difficulty}`, "load daily scores"),
    staleTime: 60_000,
  });
}

/** How long the home page waits for the preview before leaving the card plain (a cold server takes longer). */
const PREVIEW_TIMEOUT_MS = 4000;

/**
 * Today's game types, for the home page's preview. Read-only: the server never creates the day for this, so a day
 * nobody has started answers 404. Any failure means no preview, so there is no retry and no error to show.
 */
export function useDailyChallenge(date: string | null, enabled: boolean) {
  return useQuery({
    queryKey: queryKeys.dailyChallenge(date ?? ""),
    queryFn: () =>
      fetchJson<DailyChallenge>(`/api/daily-challenges/${date}`, "load daily challenge", {
        signal: AbortSignal.timeout(PREVIEW_TIMEOUT_MS),
      }),
    enabled: enabled && date !== null,
    retry: false,
    staleTime: 5 * 60_000,
  });
}

/**
 * The player's local date as `YYYY-MM-DD`. Never `toISOString()`: that is the UTC date, which is the wrong
 * day for part of every day in most of the world.
 */
export function localDateString(now: Date = new Date()): string {
  const month = String(now.getMonth() + 1).padStart(2, "0");
  const day = String(now.getDate()).padStart(2, "0");
  return `${now.getFullYear()}-${month}-${day}`;
}

/**
 * Creates a game from the day's rounds. If the server refuses the date (the device's clock is wrong, or the day
 * isn't open yet), asks again with no date, which means the server's own date. Unlike `useCreateGame`, it doesn't
 * navigate: the daily page shows the game itself, so the URL stays `/daily` and a reload works.
 */
export function useCreateDailyGame() {
  return useMutation({
    mutationFn: async ({ date, difficulty }: { date: string; difficulty: Difficulty }): Promise<DailyGameResponse> => {
      try {
        return await postJson<DailyGameResponse>("/api/daily-games", "create daily game", {
          date,
          difficulty,
        });
      } catch (error) {
        if (!hasStatus(error, 400)) throw error;
        return postJson<DailyGameResponse>("/api/daily-games", "create daily game", { difficulty });
      }
    },
  });
}
