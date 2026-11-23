import { foundAnswers, Guess, GuessClue, GuessState } from "./guessReducer";

/** One row of the round's guess list. */
export type GuessRow =
  | { kind: "found"; text: string }
  | { kind: "wrong"; text: string; clue?: GuessClue }
  | { kind: "missed"; text: string };

/**
 * The rows of the guess list: every guess in the order it was made (an answer found, or a wrong guess), then, once
 * the answers are revealed, the answers that weren't found.
 */
export function guessListRows(state: GuessState, allAnswers: string[]): GuessRow[] {
  const rows: GuessRow[] = state.guesses.map((guess: Guess) =>
    guess.answer === null
      ? guess.clue
        ? { kind: "wrong", text: guess.text, clue: guess.clue }
        : { kind: "wrong", text: guess.text }
      : { kind: "found", text: guess.answer },
  );
  const found = foundAnswers(state);
  const missed = state.gaveUp ? allAnswers.filter((answer) => !found.includes(answer)) : [];
  missed.forEach((text) => rows.push({ kind: "missed", text }));
  return rows;
}
