import type { AnswerState, GameState } from "../games/common/Model";

/** The JSON of GET /api/games/{id}/rounds/{round}, exactly as the server sends it. */
export interface GameRoundResponse {
  jsonBlob: string;
  totalRoundCount: number;
  gameType: GameTypeModel;
  answerState: AnswerState;
  /** Missing when talking to an older backend. */
  hintsUsed?: number;
  /** Missing when talking to an older backend. */
  points?: number;
  gameResult: GameResultResponse;
  /** Missing when talking to an older backend. */
  guesses?: RecordedGuess[];
  /** Missing when talking to an older backend. */
  hintsTakenAt?: number[];
}

/** A guess the server has stored for a round: a known place name as the name lists spell it. */
export interface RecordedGuess {
  text: string;
  correct: boolean;
}

/** The JSON of POST /api/games/{id}/rounds/{round}/hints. */
export interface RoundHintsResponse {
  hintsTakenAt: number[];
}

/** The JSON of POST /api/games/{id}/rounds/{round}/guesses. */
export interface RoundGuessesResponse {
  guesses: RecordedGuess[];
}

/** The JSON of POST /api/games/{id}/rounds/{round}/answers. An older backend sends a whole GameRoundResponse, which has these fields too. */
export interface RoundAnswerResponse {
  answerState: AnswerState;
  hintsUsed?: number;
  points?: number;
  gameResult: GameResultResponse;
}

export interface GameResultResponse {
  gameState: GameState;
  roundsUnanswered: number;
  /** Points on a newer backend; the number of correct rounds on an older one. */
  score: number;
  /** Missing when talking to an older backend. */
  roundAnswerStates?: AnswerState[];
  /** The rest are missing when talking to an older backend. */
  maxScore?: number;
  roundsCorrect?: number;
  perfectRounds?: number;
  roundPoints?: number[];
  roundHintsUsed?: number[];
}

export interface GameTypeModel {
  name: string;
  answerCount: number;
  /** Missing when talking to an older backend. */
  hintCount?: number;
  /** Wrong guesses allowed per round. Missing when talking to an older backend. */
  guessLimit?: number;
}

/** A round as the rest of the app uses it: the blob is parsed and the result is complete. */
export interface GameRoundView {
  gameType: Required<GameTypeModel>;
  /** The parsed jsonBlob. Each game module knows its own shape. */
  data: unknown;
  totalRoundCount: number;
  answerState: AnswerState;
  hintsUsed: number;
  points: number;
  guesses: RecordedGuess[];
  hintsTakenAt: number[];
  gameResult: {
    gameState: GameState;
    /** The sum of the round points. */
    score: number;
    maxScore: number;
    roundsCorrect: number;
    perfectRounds: number;
    roundAnswerStates: AnswerState[];
    roundPoints: number[];
    roundHintsUsed: number[];
  };
}
