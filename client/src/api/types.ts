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

/** The JSON of GET /api/daily-challenges/{date}: the game types of a stored day, in round order. */
export interface DailyChallenge {
  /** The challenge date, `YYYY-MM-DD`. */
  date: string;
  number: number;
  /** `GameType` names, one per round. */
  gameTypes: string[];
}

/** The JSON of POST /api/games and POST /api/challenges/{id}/games. */
export interface GameCreationResponse {
  id: string;
}

/** The JSON of GET /api/challenges/{id}: what a friend sees before playing. */
export interface ChallengeResponse {
  /** A `GameType` name, or `RANDOM` for a mixed game. */
  gameMode: string;
  difficulty: Difficulty;
  roundCount: number;
  /** The score to beat: the best entry's. */
  score: number;
  maxScore: number;
  /** The top of the league table, without `you`. Optional: an older server doesn't send it. */
  entries?: ChallengeEntry[];
  entryCount?: number;
}

/** One row of a challenge's league table. */
export interface ChallengeEntry {
  rank: number;
  score: number;
  /** Null until entered. */
  initials: string | null;
  /** Whether this is the asking game's row; only in a game's table. */
  you?: boolean;
}

/** The JSON of GET /api/games/{id}/challenge-table. */
export interface ChallengeTableResponse {
  challengeId: string;
  maxScore: number;
  entryCount: number;
  /** Whether this game made the challenge. */
  creator: boolean;
  /** The top five. */
  entries: ChallengeEntry[];
  /** This game's own row when it is below `entries`. */
  yours: ChallengeEntry | null;
  canEnterInitials: boolean;
}

/** The body of PUT /api/games/{id}/challenge-initials. */
export interface InitialsRequest {
  initials: string;
}

/** The JSON of POST /api/games/{id}/challenges. */
export interface ChallengeCreationResponse {
  id: string;
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
  gameResult: Omit<GameResultResponse, "roundsUnanswered">;
}
