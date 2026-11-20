import { useMutation, useQuery } from "@tanstack/react-query";
import { fetchJson, hasStatus, postJson } from "./http";
import { queryKeys } from "./queryKeys";
import type { DailyGameResponse, DailyScore } from "./types";

/** The day's totals for the daily challenge. They go stale after a minute as others finish; no polling. */
export function useDailyScore(date: string) {
  return useQuery({
    queryKey: queryKeys.dailyScore(date),
    queryFn: () => fetchJson<DailyScore>(`/api/daily-scores/${date}`, "load daily scores"),
    staleTime: 60_000,
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
    mutationFn: async ({ date, hardMode }: { date: string; hardMode: boolean }): Promise<DailyGameResponse> => {
      try {
        return await postJson<DailyGameResponse>("/api/daily-games", "create daily game", {
          date,
          hardMode,
        });
      } catch (error) {
        if (!hasStatus(error, 400)) throw error;
        return postJson<DailyGameResponse>("/api/daily-games", "create daily game", { hardMode });
      }
    },
  });
}
