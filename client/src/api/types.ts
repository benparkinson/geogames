import type { AnswerState, GameState } from "../games/common/Model";

/** How a game is played, chosen when it is created and never changed. */
export type Difficulty = "EASY" | "NORMAL" | "HARD";

/** The JSON of GET /api/games/{id}/rounds/{round}, exactly as the server sends it. */
export interface GameRoundResponse {
  jsonBlob: string;
  totalRoundCount: number;
  gameType: GameTypeModel;
  answerState: AnswerState;
  hintsUsed: number;
  points: number;
  gameResult: GameResultResponse;
  guesses: RecordedGuess[];
  hintsTakenAt: number[];
  /** Kept by the server for old clients (whether the difficulty is HARD). Not read. */
  hardMode: boolean;
  /** Chosen when the game was created and never changes. */
  difficulty: Difficulty;
  /** The continents to show from the start of the round: empty unless the game is Easy. */
  continents: string[];
}

/** A guess the server has stored for a round: a known place name as the name lists spell it. */
export interface RecordedGuess {
  text: string;
  correct: boolean;
}

/** The JSON of GET /api/daily-scores/{date}: one day's totals for the daily challenge. */
export interface DailyScore {
  /** The challenge date, `YYYY-MM-DD`. */
  date: string;
  /** The league the totals are for. */
  difficulty: Difficulty;
  /** Finished games counted. */
  players: number;
  /** The mean score to one decimal place; null when `players` is 0. */
  averageScore: number | null;
  /** The best possible score. */
  maxScore: number;
  /** 11 counts: tenths of `maxScore`, the last one a perfect score. */
  buckets: number[];
}

/** The JSON of POST /api/daily-games: a new game made from the day's rounds. */
export interface DailyGameResponse {
  id: string;
  /** The date the rounds belong to, `YYYY-MM-DD`. */
  date: string;
  /** "Daily 2026-11-18". */
  number: number;
}

/** The JSON of POST /api/games/{id}/rounds/{round}/hints. */
export interface RoundHintsResponse {
  hintsTakenAt: number[];
}

/** The JSON of POST /api/games/{id}/rounds/{round}/guesses. */
export interface RoundGuessesResponse {
  guesses: RecordedGuess[];
}

/** The JSON of POST /api/games/{id}/rounds/{round}/answers: the round's outcome and the game's result, with no round data. */
export interface RoundAnswerResponse {
  answerState: AnswerState;
  hintsUsed: number;
  points: number;
  gameResult: GameResultResponse;
}

export interface GameResultResponse {
  gameState: GameState;
  roundsUnanswered: number;
  /** The sum of the round points. */
  score: number;
  maxScore: number;
  roundsCorrect: number;
  perfectRounds: number;
  roundAnswerStates: AnswerState[];
  roundPoints: number[];
  roundHintsUsed: number[];
  /** Per round: correct with no hints and no wrong guess. */
  roundFirstGuess: boolean[];
  firstGuessRounds: number;
}

export interface GameTypeModel {
  name: string;
  answerCount: number;
  hintCount: number;
  /** Wrong guesses allowed per round. */
  guessLimit: number;
}

/** A round as the rest of the app uses it: the blob is parsed. */
export interface GameRoundView {
  gameType: GameTypeModel;
  /** The parsed jsonBlob. Each game module knows its own shape. */
  data: unknown;
  totalRoundCount: number;
  answerState: AnswerState;
  hintsUsed: number;
  points: number;
  guesses: RecordedGuess[];
  hintsTakenAt: number[];
  /** Fixed when the game was created: Hard hides the distance clues for all of it, Easy shows the continent. */
  difficulty: Difficulty;
  /** The continents to show from the start of the round: empty unless the game is Easy. */
  continents: string[];
  /** The app doesn't read `perfectRounds`: the server still sends it for the deployed client. */
  gameResult: Omit<GameResultResponse, "roundsUnanswered" | "perfectRounds">;
}
