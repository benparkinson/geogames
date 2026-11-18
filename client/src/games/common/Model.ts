import type { RecordedGuess } from "../../api/types";

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

/** How a round that has just ended was answered: out of guesses, given up, or solved. */
export function finalAnswerState(
  gaveUp: boolean,
  outOfGuesses: boolean,
): typeof CORRECT_ANSWER | typeof GAVE_UP | typeof OUT_OF_GUESSES {
  return outOfGuesses ? OUT_OF_GUESSES : gaveUp ? GAVE_UP : CORRECT_ANSWER;
}

/** What the client reports to the server when a round ends. */
export interface RoundAnswer {
  answerState: AnswerState;
  hintsUsed: number;
  /** Wrong guesses that counted against the guess limit, as `wrongGuessCount` in `guessReducer` counts them. */
  wrongGuesses: number;
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
  /** Wrong guesses allowed this round, from the backend. */
  guessLimit: number;
  /** The hints the server has recorded for this round. 0 until the round is answered. */
  hintsUsed: number;
  /** The guesses the server has recorded for this round, in order. */
  guesses: RecordedGuess[];
  /** For each hint the server recorded before the answer: how many guesses had been made when it was taken. */
  hintsTakenAt: number[];
  /** Saves a hint as soon as it is taken, so a refresh keeps it. */
  recordHint: (hint: { hint: number; guessesBefore: number }) => void;
  /** Saves a guess that the guess reducer recorded, so a refresh restores it. */
  recordGuess: (guess: RecordedGuess) => void;
  /** This round's points, as the server last saw them. */
  points: number;
  /** The game's points so far, as the server last saw them. */
  score: number;
  maxScore: number;
  perfectRounds: number;
  gameCompleted: boolean;
  roundAnswerStates: AnswerState[];
  roundPoints: number[];
  /** Set when this round is part of a daily challenge: the title shows "Daily <date>". */
  dailyDate?: string;
}
