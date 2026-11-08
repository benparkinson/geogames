import type { PlaceKind } from "./placeNames";

export const queryKeys = {
  placeNames: (kind: PlaceKind) => ["placeNames", kind] as const,
  game: (gameId: string) => ["games", gameId] as const,
  round: (gameId: string, round: number) => [...queryKeys.game(gameId), "rounds", round] as const,
};
