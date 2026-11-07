import { normaliseString } from "../../helper/stringHelper";
import { AnswerCandidate, matchAnswer } from "./answers";
import { AnswerState, CORRECT_ANSWER, GAVE_UP, UNANSWERED } from "./Model";

/** One thing the player typed. `answer` is the canonical answer it matched, or null if it was wrong. */
export interface Guess {
  text: string;
  answer: string | null;
}

export interface GuessState {
  guesses: Guess[];
  gaveUp: boolean;
  over: boolean;
  hintsUsed: number;
}

export type GuessAction =
  | { type: "guess"; text: string }
  | { type: "giveUp" }
  | { type: "hint" };

export type GuessResult =
  | { kind: "empty" }
  | { kind: "duplicate"; guess: string }
  | { kind: "unknown"; guess: string }
  | { kind: "wrong"; guess: string }
  | { kind: "correct"; remaining: number }
  | { kind: "over" };

export interface GuessConfig {
  answers: AnswerCandidate[];
  answerCount: number;
  /** When set, a guess it rejects is reported as "unknown" instead of wrong. */
  isKnownGuess?: (guess: string) => boolean;
  hintsTotal: number;
}

export interface GuessOutcome {
  state: GuessState;
  /** The feedback for a "guess" action. */
  result?: GuessResult;
  /** Set when this action ended the round, so the caller can report it to the server. */
  finished?: typeof CORRECT_ANSWER | typeof GAVE_UP;
}

function allFound(answers: AnswerCandidate[]): Guess[] {
  return answers.map(a => ({ text: a.name, answer: a.name }));
}

export function initialGuessState(answerState: AnswerState, answers: AnswerCandidate[]): GuessState {
  if (answerState === UNANSWERED) {
    return { guesses: [], gaveUp: false, over: false, hintsUsed: 0 };
  }
  if (answerState === GAVE_UP) {
    // The server doesn't keep the guesses, so every answer shows as revealed rather than found.
    return { guesses: [], gaveUp: true, over: true, hintsUsed: 0 };
  }
  return { guesses: allFound(answers), gaveUp: false, over: true, hintsUsed: 0 };
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
        state: { ...state, gaveUp: true, over: true },
        finished: GAVE_UP,
      };
    case "hint":
      if (state.over || state.hintsUsed >= config.hintsTotal) {
        return { state };
      }
      return { state: { ...state, hintsUsed: state.hintsUsed + 1 } };
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

  const guesses = [...state.guesses, { text: trimmed, answer: matched }];
  if (matched === null) {
    return { state: { ...state, guesses }, result: { kind: "wrong", guess: trimmed } };
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
