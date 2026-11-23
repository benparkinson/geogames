import { AnswerCandidate, matchAnswer } from "./answers";
import { DIFFICULTY_LABEL } from "./difficultyLabel";
import { Difficulty, GameRoundView } from "../../api/types";
import { findGameModule, RANDOM_OPTION } from "../registry";
import { CORRECT_ANSWER, OUT_OF_GUESSES } from "./Model";
import { OUTCOME_ORDER, OUTCOME_STYLES, roundOutcome, RoundOutcome } from "./outcome";
import type { RoundResultKind } from "./roundView";

/** The game type to start for "Play again": the one every round used, or a mix if they differed. */
export function playAgainType(rounds: GameRoundView[]): string {
  const types = new Set(rounds.map((r) => r.gameType.name));
  return types.size === 1 ? rounds[0].gameType.name : RANDOM_OPTION.type;
}

function roundLabel(round: GameRoundView): string {
  return findGameModule(round.gameType.name)?.singular ?? round.gameType.name;
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
  /** The round's recorded guesses that were not answers, in the order they were made. */
  wrongGuessNames: string[];
}

export interface GameSummaryView {
  score: number;
  maxScore: number;
  /** "perfect": every round a first guess; "fullMarks": full marks after some wrong guesses. */
  heading: "perfect" | "fullMarks" | "scored";
  summaryLine: string;
  playAgainType: string;
  /** A mixed game names each round's type; a game of one type names it once, at the top. */
  mixed: boolean;
  gameTitle: string | null;
  difficulty: Difficulty;
  /** Rounds answered correctly, with or without hints. */
  solved: number;
  wrongGuesses: number;
  hintsUsed: number;
  rows: SummaryRowView[];
}

/** Everything the results page shows, worked out from the game's rounds. `rounds` is not empty. */
export function summariseGame(rounds: GameRoundView[]): GameSummaryView {
  // Every round carries the whole game's result.
  const { score, maxScore, roundAnswerStates, roundPoints, roundHintsUsed, roundFirstGuess } = rounds[0].gameResult;
  const counts: Record<RoundOutcome, number> = { firstGuess: 0, correct: 0, hints: 0, missed: 0 };
  roundAnswerStates.forEach((state, i) => {
    const outcome = roundOutcome(state, roundHintsUsed[i], roundFirstGuess[i], roundPoints[i]);
    if (outcome) counts[outcome]++;
  });
  const perfect = counts.firstGuess === rounds.length;
  const fullMarks = !perfect && counts.firstGuess + counts.correct === rounds.length;
  const breakdown = OUTCOME_ORDER.filter((o) => counts[o] > 0).map(
    (o) => `${counts[o]} ${counts[o] === 1 ? OUTCOME_STYLES[o].singular : OUTCOME_STYLES[o].plural}`,
  );
  const difficulty = rounds[0].difficulty;
  const summaryLine = [...(breakdown.length > 0 ? breakdown : ["0 correct"]), ...DIFFICULTY_LABEL[difficulty]].join(
    " · ",
  );
  const type = playAgainType(rounds);
  const mixed = type === RANDOM_OPTION.type;
  const rows = rounds.map((round, i): SummaryRowView => {
    const outcome = roundOutcome(round.answerState, round.hintsUsed, round.gameResult.roundFirstGuess[i], round.points);
    const result: RoundResultKind =
      outcome === "firstGuess"
        ? "firstGuess"
        : round.answerState === CORRECT_ANSWER
          ? "correct"
          : round.answerState === OUT_OF_GUESSES
            ? "outOfGuesses"
            : "revealed";
    const answers = roundAnswers(round);
    return {
      label: roundLabel(round),
      answers: answers.map((a) => a.name),
      missed: missedAnswers(round, answers),
      outcome,
      result,
      hintsUsed: round.hintsUsed,
      outOfGuesses: round.answerState === OUT_OF_GUESSES,
      points: round.points,
      wrongGuessNames: round.guesses.filter((g) => !g.correct).map((g) => g.text),
    };
  });
  return {
    score,
    maxScore,
    heading: perfect ? "perfect" : fullMarks ? "fullMarks" : "scored",
    summaryLine,
    playAgainType: type,
    mixed,
    gameTitle: mixed ? null : (findGameModule(type)?.title ?? type),
    difficulty,
    solved: roundAnswerStates.filter((state) => state === CORRECT_ANSWER).length,
    wrongGuesses: rows.reduce((sum, row) => sum + row.wrongGuessNames.length, 0),
    hintsUsed: rows.reduce((sum, row) => sum + row.hintsUsed, 0),
    rows,
  };
}
