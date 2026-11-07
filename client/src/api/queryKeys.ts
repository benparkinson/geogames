export const queryKeys = {
  countries: ["countries"] as const,
  round: (gameId: string, round: number) => ["games", gameId, "rounds", round] as const,
};
