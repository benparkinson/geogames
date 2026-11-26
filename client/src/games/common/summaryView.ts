import { AnswerCandidate, matchAnswer } from "./answers";
import { Difficulty, GameRoundView } from "../../api/types";
import { findGameModule, RANDOM_OPTION } from "../registry";
import { CORRECT_ANSWER, OUT_OF_GUESSES } from "./Model";
import { roundOutcome, RoundOutcome } from "./outcome";
import { roundResultKind, RoundResultKind } from "./roundView";
import { MAX_ROUND_POINTS, pointsBreakdown } from "./scoring";
import type { PointsBreakdown } from "./scoring";

/** The game type to start for "Play another": the one every round used, or a mix if they differed. */
export function playAgainType(rounds: GameRoundView[]): string {
  const types = new Set(rounds.map((r) => r.gameType.name));
  return types.size === 1 ? rounds[0].gameType.name : RANDOM_OPTION.type;
}

function roundLabel(round: GameRoundView): string {
  return findGameModule(round.gameType.name)?.title ?? round.gameType.name;
}

/** The answers of a round, for the summary. */
function roundAnswers(round: GameRoundView): AnswerCandidate[] {
  return findGameModule(round.gameType.name)?.answers(round.data) ?? [];
}

/**
 * The answers of a round that the player didn't find: none for a correct round, otherwise every answer no correct
 * guess matched (a guess is stored as typed, so an alias such as Holland finds the Netherlands).
 */
function missedAnswers(round: GameRoundView, answers: AnswerCandidate[]): string[] {
  if (round.answerState === CORRECT_ANSWER) return [];
  const found = new Set(round.guesses.filter((g) => g.correct).map((g) => matchAnswer(g.text, answers)));
  return answers.map((a) => a.name).filter((name) => !found.has(name));
}

/** One round of the results page. */
export interface SummaryRowView {
  /** The round's type name, for a mixed game. */
  label: string;
  answers: string[];
  /** The answers the player didn't find, shown as missed. */
  missed: string[];
  outcome: RoundOutcome | null;
  result: RoundResultKind;
  hintsUsed: number;
  outOfGuesses: boolean;
  points: number;
  breakdown: PointsBreakdown;
  /** The round's recorded guesses that were not answers, in the order they were made. */
  wrongGuessNames: string[];
}

export interface GameSummaryView {
  score: number;
  maxScore: number;
  /** "perfect": every round earned all 10 points. */
  heading: "perfect" | "scored";
  /** Rounds that earned all 10 points. */
  perfectRounds: number;
  playAgainType: string;
  /** A mixed game names each round's type; a game of one type names it once, at the top. */
  mixed: boolean;
  gameTitle: string | null;
  difficulty: Difficulty;
  hintsUsed: number;
  rows: SummaryRowView[];
}

/** Everything the results page shows, worked out from the game's rounds. `rounds` is not empty. */
export function summariseGame(rounds: GameRoundView[]): GameSummaryView {
  // Every round carries the whole game's result.
  const { score, maxScore, roundPoints } = rounds[0].gameResult;
  const perfectRounds = roundPoints.filter((points) => points >= MAX_ROUND_POINTS).length;
  const perfect = perfectRounds === rounds.length;
  const type = playAgainType(rounds);
  const mixed = type === RANDOM_OPTION.type;
  const rows = rounds.map((round): SummaryRowView => {
    const outcome = roundOutcome(round.answerState, round.points);
    const result = roundResultKind(round.answerState);
    const answers = roundAnswers(round);
    const missed = missedAnswers(round, answers);
    const wrongGuessNames = round.guesses.filter((g) => !g.correct).map((g) => g.text);
    return {
      label: roundLabel(round),
      answers: answers.map((a) => a.name),
      missed,
      outcome,
      result,
      hintsUsed: round.hintsUsed,
      outOfGuesses: round.answerState === OUT_OF_GUESSES,
      points: round.points,
      breakdown: pointsBreakdown(
        round.answerState,
        round.hintsUsed,
        answers.length - missed.length,
        answers.length,
        wrongGuessNames.length,
      ),
      wrongGuessNames,
    };
  });
  return {
    score,
    maxScore,
    heading: perfect ? "perfect" : "scored",
    perfectRounds,
    playAgainType: type,
    mixed,
    gameTitle: mixed ? null : (findGameModule(type)?.title ?? type),
    difficulty: rounds[0].difficulty,
    hintsUsed: rows.reduce((sum, row) => sum + row.hintsUsed, 0),
    rows,
  };
}
