import { normaliseString } from "../../helper/stringHelper";
import { AnswerCandidate, matchAnswer } from "./answers";
import type { RecordedGuess } from "../../api/types";
import { AnswerState, CORRECT_ANSWER, GAVE_UP, OUT_OF_GUESSES, UNANSWERED } from "./Model";

/** One thing the player typed. `answer` is the canonical answer it matched, or null if it was wrong. */
export interface Guess {
  text: string;
  answer: string | null;
  /** For a wrong guess in a game with clues: how far, and which way, to the nearest answer not yet found. */
  clue?: GuessClue;
}

/** Worked out once, when the guess is made. `bearing` is degrees from north, from the guess to the answer. */
export interface GuessClue {
  km: number;
  bearing: number;
}

export interface GuessState {
  guesses: Guess[];
  /** The answers are shown as revealed: true when the player gave up and when they ran out of guesses. */
  gaveUp: boolean;
  /** The round ended because the wrong guesses ran out (`gaveUp` is true too). */
  outOfGuesses: boolean;
  over: boolean;
  hintsUsed: number;
  /** One entry per hint taken: the canonical answers found at the moment that hint was taken. */
  foundAtHint: string[][];
}

export type GuessAction =
  | { type: "guess"; text: string }
  | { type: "giveUp" }
  | { type: "hint" };

export type GuessResult =
  | { kind: "empty" }
  | { kind: "duplicate"; guess: string }
  | { kind: "unknown"; guess: string }
  | { kind: "wrong"; guess: string; guessesLeft: number; clue?: GuessClue }
  | { kind: "outOfGuesses"; guess: string; clue?: GuessClue }
  | { kind: "correct"; remaining: number }
  | { kind: "over" };

export interface GuessConfig {
  answers: AnswerCandidate[];
  answerCount: number;
  /** When set, a guess it rejects is reported as "unknown" instead of wrong. */
  isKnownGuess?: (guess: string) => boolean;
  hintsTotal: number;
  /** How many wrong guesses the round allows. Infinity means no limit. */
  guessLimit: number;
  /**
   * For a wrong guess: the clue towards the nearest of `targets`, the canonical answers not found yet.
   * Null when the guess can't be located. Must be deterministic: the reducer stays pure.
   */
  clueFor?: (guess: string, targets: string[]) => GuessClue | null;
}

export interface GuessOutcome {
  state: GuessState;
  /** The feedback for a "guess" action. */
  result?: GuessResult;
  /** Set when this action ended the round, so the caller can report it to the server. */
  finished?: typeof CORRECT_ANSWER | typeof GAVE_UP | typeof OUT_OF_GUESSES;
}

function allFound(answers: AnswerCandidate[]): Guess[] {
  return answers.map(a => ({ text: a.name, answer: a.name }));
}

/** How many wrong guesses have been used. Duplicates, unknown and empty guesses aren't recorded, so they don't count. */
export function wrongGuessCount(state: GuessState): number {
  return state.guesses.filter(g => g.answer === null).length;
}

export function guessesLeft(state: GuessState, guessLimit: number): number {
  return Math.max(0, guessLimit - wrongGuessCount(state));
}

const EMPTY_STATE: GuessState = { guesses: [], gaveUp: false, outOfGuesses: false, over: false, hintsUsed: 0, foundAtHint: [] };

/**
 * Rebuilds a round's state from what the server holds: the answer state, the hints that counted (`hintsUsed`, set
 * once the round is answered), the guesses saved while it was played and, for each hint taken, how many guesses
 * came before it (`hintsTakenAt`).
 *
 * The saved guesses are replayed through `guessReducer`, which works out each one's answer again with `matchAnswer`
 * (that is trusted over the stored `correct`). `isKnownGuess` is left out: the server only stored known names, and
 * the place names may not have loaded yet. For an unanswered round, `finished` is set when the replay ended the
 * round, which means the answer request that followed the last guess was lost and must be sent again.
 */
export function restoreGuessState(
  answerState: AnswerState,
  hintsUsed: number,
  saved: RecordedGuess[],
  hintsTakenAt: number[],
  config: GuessConfig,
): { state: GuessState; finished?: GuessOutcome["finished"] } {
  const replayConfig: GuessConfig = { ...config, isKnownGuess: undefined };
  let state = EMPTY_STATE;
  let finished: GuessOutcome["finished"];
  let nextHint = 0;
  // Guesses and hints are replayed in the order the player made them: hint i was taken after hintsTakenAt[i]
  // guesses, which is what decides foundAtHint. Hints left over at the end come after the last guess.
  for (let g = 0; g <= saved.length; g++) {
    while (nextHint < hintsTakenAt.length && (hintsTakenAt[nextHint] <= g || g === saved.length)) {
      state = guessReducer(state, { type: "hint" }, replayConfig).state;
      nextHint++;
    }
    if (g === saved.length || state.over) {
      continue;
    }
    const outcome = guessReducer(state, { type: "guess", text: saved[g].text }, replayConfig);
    state = outcome.state;
    finished = outcome.finished;
  }
  if (answerState === UNANSWERED) {
    return finished ? { state, finished } : { state };
  }
  // The hints that counted are the server's. Where the replay knows what was found at a hint, use it.
  const foundAtHint: string[][] = Array.from({ length: hintsUsed }, (_, i) => state.foundAtHint[i] ?? []);
  if (answerState === GAVE_UP || answerState === OUT_OF_GUESSES) {
    // The answers found before giving up show as found, and the rest as revealed.
    return {
      state: { guesses: state.guesses, gaveUp: true, outOfGuesses: answerState === OUT_OF_GUESSES, over: true, hintsUsed, foundAtHint },
    };
  }
  const wrong = state.guesses.filter(g => g.answer === null);
  return {
    state: {
      guesses: [...wrong, ...allFound(config.answers)],
      gaveUp: false,
      outOfGuesses: false,
      over: true,
      hintsUsed,
      foundAtHint,
    },
  };
}

export function guessReducer(state: GuessState, action: GuessAction, config: GuessConfig): GuessOutcome {
  switch (action.type) {
    case "guess":
      return applyGuess(state, action.text, config);
    case "giveUp":
      if (state.over) {
        return { state };
      }
      // Keep the guesses as they are: the answers not found yet are shown as revealed, not found.
      return {
        state: { ...state, gaveUp: true, outOfGuesses: false, over: true },
        finished: GAVE_UP,
      };
    case "hint":
      if (state.over || state.hintsUsed >= config.hintsTotal) {
        return { state };
      }
      const found = state.guesses.flatMap(g => (g.answer === null ? [] : [g.answer]));
      return { state: { ...state, hintsUsed: state.hintsUsed + 1, foundAtHint: [...state.foundAtHint, found] } };
  }
}

function applyGuess(state: GuessState, text: string, config: GuessConfig): GuessOutcome {
  if (state.over) {
    return { state, result: { kind: "over" } };
  }

  const trimmed = text.trim();
  if (trimmed === "") {
    return { state, result: { kind: "empty" } };
  }

  const normalised = normaliseString(trimmed);
  const matched = matchAnswer(trimmed, config.answers);

  const typedBefore = state.guesses.some(g => normaliseString(g.text) === normalised);
  const foundBefore = matched !== null && state.guesses.some(g => g.answer === matched);
  if (typedBefore || foundBefore) {
    // Name an alias by what it stands for, so "Deutschland" after "Germany" says Germany.
    const isAlias = matched !== null && normaliseString(matched) !== normalised;
    return { state, result: { kind: "duplicate", guess: isAlias ? matched : trimmed } };
  }

  if (config.isKnownGuess && !config.isKnownGuess(trimmed)) {
    return { state, result: { kind: "unknown", guess: trimmed } };
  }

  let clue: GuessClue | undefined;
  if (matched === null && config.clueFor) {
    const found = state.guesses.flatMap(g => (g.answer === null ? [] : [g.answer]));
    const unfound = config.answers.map(a => a.name).filter(name => !found.includes(name));
    clue = config.clueFor(trimmed, unfound) ?? undefined;
  }
  const guesses = [...state.guesses, clue ? { text: trimmed, answer: matched, clue } : { text: trimmed, answer: matched }];
  if (matched === null) {
    const left = config.guessLimit - wrongGuessCount({ ...state, guesses });
    if (left <= 0) {
      // Keep the correct guesses found so far: the other answers are shown as revealed.
      return {
        state: { ...state, guesses, gaveUp: true, outOfGuesses: true, over: true },
        result: { kind: "outOfGuesses", guess: trimmed, clue },
        finished: OUT_OF_GUESSES,
      };
    }
    return { state: { ...state, guesses }, result: { kind: "wrong", guess: trimmed, guessesLeft: left, clue } };
  }

  const remaining = config.answerCount - guesses.filter(g => g.answer !== null).length;
  if (remaining <= 0) {
    return {
      state: { ...state, guesses, over: true },
      result: { kind: "correct", remaining: 0 },
      finished: CORRECT_ANSWER,
    };
  }
  return { state: { ...state, guesses }, result: { kind: "correct", remaining } };
}
