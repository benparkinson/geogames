export interface MapProps<Type> {
  data: Type;
  gaveUp: boolean;
  gameOver: boolean;
  /** How many hints the player has taken so far. */
  hintsUsed: number;
}
