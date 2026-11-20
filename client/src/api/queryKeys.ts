import type { PlaceKind } from "./placeNames";

export const queryKeys = {
  placeNames: (kind: PlaceKind) => ["placeNames", kind] as const,
  appVersion: () => ["appVersion"] as const,
  game: (gameId: string) => ["games", gameId] as const,
  dailyScore: (date: string, difficulty: string) => ["dailyScore", date, difficulty] as const,
  round: (gameId: string, round: number) => [...queryKeys.game(gameId), "rounds", round] as const,
};
