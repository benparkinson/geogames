import type { AnswerState, GameState } from "../games/common/Model";

/** The JSON of GET /api/games/{id}/rounds/{round}, exactly as the server sends it. */
export interface GameRoundResponse {
  jsonBlob: string;
  totalRoundCount: number;
  gameType: GameTypeModel;
  answerState: AnswerState;
  gameResult: GameResultResponse;
}

export interface GameResultResponse {
  gameState: GameState;
  roundsUnanswered: number;
  score: number;
  /** Missing when talking to an older backend. */
  roundAnswerStates?: AnswerState[];
}

export interface GameTypeModel {
  name: string;
  answerCount: number;
}

/** A round as the rest of the app uses it: the blob is parsed and the result is complete. */
export interface GameRoundView {
  gameType: GameTypeModel;
  /** The parsed jsonBlob. Each game module knows its own shape. */
  data: unknown;
  totalRoundCount: number;
  answerState: AnswerState;
  gameResult: {
    gameState: GameState;
    score: number;
    roundAnswerStates: AnswerState[];
  };
}
