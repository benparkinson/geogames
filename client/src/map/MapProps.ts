export interface MapProps<Type> {
  data: Type;
  gaveUp: boolean;
  gameOver: boolean;
  /** How many hints the player has taken so far. */
  hintsUsed: number;
  /** For each hint taken, the answers the player had found when they took it. */
  foundAtHint?: string[][];
}
