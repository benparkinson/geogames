export type AnswerState = "UNANSWERED" | "CORRECT" | "GAVE_UP" | "OUT_OF_GUESSES";
export type GameState = "CREATED" | "IN_PROGRESS" | "FINISHED";

export const CORRECT_ANSWER = "CORRECT" as const satisfies AnswerState;
export const GAVE_UP = "GAVE_UP" as const satisfies AnswerState;
export const OUT_OF_GUESSES = "OUT_OF_GUESSES" as const satisfies AnswerState;
export const UNANSWERED = "UNANSWERED" as const satisfies AnswerState;
export const FINISHED = "FINISHED" as const satisfies GameState;

/** The round ended without being solved, so its answers are shown as revealed. */
export function isRevealed(state: AnswerState): boolean {
  return state === GAVE_UP || state === OUT_OF_GUESSES;
}

/** What the client reports to the server when a round ends. */
export interface RoundAnswer {
  answerState: AnswerState;
  hintsUsed: number;
}

export interface Round {
  nextRound: () => void;
  goToRound: (round: number) => void;
  showSummary: () => void;
  hasNextRound: boolean;
  currentRound: number;
  totalRoundCount: number;
  answerState: AnswerState;
  /** How many hints this game type offers (from the backend). */
  hintCount: number;
  /** Wrong guesses allowed this round, from the backend. Infinity means no limit. */
  guessLimit: number;
  /** The hints the server has recorded for this round. 0 until the round is answered. */
  hintsUsed: number;
  /** This round's points, as the server last saw them. */
  points: number;
  /** The game's points so far, as the server last saw them. */
  score: number;
  maxScore: number;
  perfectRounds: number;
  gameCompleted: boolean;
  roundAnswerStates: AnswerState[];
  roundPoints: number[];
}
