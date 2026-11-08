export type AnswerState = "UNANSWERED" | "CORRECT" | "GAVE_UP";
export type GameState = "CREATED" | "IN_PROGRESS" | "FINISHED";

export const CORRECT_ANSWER = "CORRECT" as const satisfies AnswerState;
export const GAVE_UP = "GAVE_UP" as const satisfies AnswerState;
export const UNANSWERED = "UNANSWERED" as const satisfies AnswerState;
export const FINISHED = "FINISHED" as const satisfies GameState;

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
