import { foundAnswers, GuessState, wrongGuessCount } from "./guessReducer";
import type { HintConfig } from "./hints";
import { AnswerState, CORRECT_ANSWER, finalAnswerState, OUT_OF_GUESSES, Round, UNANSWERED } from "./Model";
import { roundOutcome, RoundOutcome } from "./outcome";
import { pointsBreakdown, PointsBreakdown, roundPoints } from "./scoring";

/** How a finished round ended, for choosing its message. */
export type RoundResultKind = "correct" | "revealed" | "outOfGuesses";

/** How a round with this final answer state ended. Not for an unanswered round. */
export function roundResultKind(answerState: AnswerState): RoundResultKind {
  if (answerState === CORRECT_ANSWER) return "correct";
  return answerState === OUT_OF_GUESSES ? "outOfGuesses" : "revealed";
}

/** Everything the round screen shows that is worked out from the round's state, so the components only render. */
export interface RoundView {
  /** `UNANSWERED` while the round is being played. */
  answerState: AnswerState;
  /** While playing: what the round is worth now (correct with the hints used so far). Once over: what it scored. */
  points: number;
  /** The points split into the score bar's parts. */
  breakdown: PointsBreakdown;
  /** How the round ended; null while it is being played. */
  outcome: RoundOutcome | null;
  /** Which message the banner shows; null while the round is being played. */
  result: RoundResultKind | null;
  hintsUsed: number;
  answersFound: number;
  answerCount: number;
  /** Wrong guesses that counted against the guess limit: none and no hints makes a won round a first guess. */
  wrongGuesses: number;
  /** The text of each hint taken that has text, so a reopened round shows them. */
  hintTexts: string[];
  /** "Continent: Europe" for an Easy round, otherwise null. */
  continentText: string | null;
}

/** "Continent: Europe", or "Continents: Europe / Asia" for a country that spans two. */
export function continentText(continents: string[]): string | null {
  if (continents.length === 0) return null;
  return `${continents.length === 1 ? "Continent" : "Continents"}: ${continents.join(" / ")}`;
}

export function roundView(round: Round, state: GuessState, answerCount: number, hints: HintConfig): RoundView {
  const { hintsUsed } = state;
  const answersFound = foundAnswers(state).length;
  const wrongGuesses = wrongGuessCount(state);
  const answerState = state.over ? finalAnswerState(state.gaveUp, state.outOfGuesses) : UNANSWERED;
  const scored = state.over ? answerState : CORRECT_ANSWER;
  const points = roundPoints(scored, hintsUsed, answersFound, answerCount, wrongGuesses);
  const outcome = state.over ? roundOutcome(answerState, points) : null;
  const result = state.over ? roundResultKind(answerState) : null;
  const hintTexts = Array.from({ length: hintsUsed }, (_, i) => hints.text?.(i, state.foundAtHint)).filter(
    (text): text is string => !!text,
  );
  return {
    answerState,
    points,
    breakdown: pointsBreakdown(scored, hintsUsed, answersFound, answerCount, wrongGuesses),
    outcome,
    result,
    hintsUsed,
    answersFound,
    answerCount,
    wrongGuesses,
    hintTexts,
    continentText: continentText(round.continents),
  };
}

/** The top bar's view of the game: the round in progress merged into the server's results. */
export interface LiveProgress {
  states: AnswerState[];
  points: number[];
  outcomes: (RoundOutcome | null)[];
  score: number;
}

/** Reflects the local state of the current round straight away, without waiting for the server. Does not mutate `round`. */
export function liveProgress(round: Round, view: RoundView): LiveProgress {
  const states = round.roundAnswerStates.slice();
  const points = round.roundPoints.slice();
  const outcomes = states.map((state, i) => roundOutcome(state, points[i]));
  let score = round.score;
  if (view.answerState !== UNANSWERED) {
    states[round.currentRound] = view.answerState;
    points[round.currentRound] = view.points;
    outcomes[round.currentRound] = view.outcome;
    if (round.answerState === UNANSWERED) {
      // The round ended here and the server's copy hasn't been updated yet.
      score += view.points;
    }
  }
  return { states, points, outcomes, score };
}
