export type AnswerState = "UNANSWERED" | "CORRECT" | "GAVE_UP";
export type GameState = "CREATED" | "IN_PROGRESS" | "FINISHED";

export const CORRECT_ANSWER = "CORRECT" as const satisfies AnswerState;
export const GAVE_UP = "GAVE_UP" as const satisfies AnswerState;
export const UNANSWERED = "UNANSWERED" as const satisfies AnswerState;
export const FINISHED = "FINISHED" as const satisfies GameState;

export interface Round {
  nextRound: () => void;
  goToRound: (round: number) => void;
  showSummary: () => void;
  hasNextRound: boolean;
  currentRound: number;
  totalRoundCount: number;
  answerState: AnswerState;
  score: number;
  gameCompleted: boolean;
  roundAnswerStates: AnswerState[];
}
