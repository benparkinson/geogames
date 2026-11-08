import type { PlaceKind } from "./placeNames";

export const queryKeys = {
  placeNames: (kind: PlaceKind) => ["placeNames", kind] as const,
  round: (gameId: string, round: number) => ["games", gameId, "rounds", round] as const,
};
