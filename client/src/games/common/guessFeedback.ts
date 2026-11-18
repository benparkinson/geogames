import { GuessResult } from "./guessReducer";

/** What to tell the player after a guess, or null for nothing. */
export function feedbackText(result: GuessResult, answerNoun: string): string | null {
  switch (result.kind) {
    case "empty":
      return "Type a guess first.";
    case "duplicate":
      return `You've already guessed ${result.guess}.`;
    case "unknown":
      return `We don't recognise "${result.guess}" as a ${answerNoun}. Check the spelling.`;
    case "wrong":
      // The wrong-guess chip shows the guess and its clue.
      return null;
    case "outOfGuesses":
      // The round is over: RoundResult says so, and the feedback line is hidden.
      return null;
    case "correct":
      return result.remaining > 0 ? `Correct! ${result.remaining} to go.` : "Correct!";
    case "over":
      return null;
  }
}
