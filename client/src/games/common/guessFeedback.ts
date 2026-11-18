import { compassPoint, formatKm } from "./distance";
import { GuessClue, GuessResult } from "./guessReducer";

/** What to tell the player after a guess, or null for nothing. */
export function feedbackText(
  result: GuessResult,
  answerNoun: string,
  answerCount: number,
  showClues: boolean,
): string | null {
  switch (result.kind) {
    case "empty":
      return "Type a guess first.";
    case "duplicate":
      return `You've already guessed ${result.guess}.`;
    case "unknown":
      return `We don't recognise "${result.guess}" as a ${answerNoun}. Check the spelling.`;
    case "wrong":
      return showClues && result.clue
        ? `Not ${result.guess} · ${clueText(result.clue, answerCount)}`
        : `Not ${result.guess}.`;
    case "outOfGuesses":
      // The round is over: RoundResult says so, and the feedback line is hidden.
      return null;
    case "correct":
      return result.remaining > 0 ? `Correct! ${result.remaining} to go.` : "Correct!";
    case "over":
      return null;
  }
}

/** "1,240 km north-east". With several answers it is the nearest one still to find: "nearest 480 km north". */
export function clueText(clue: GuessClue, answerCount: number): string {
  const where = `${formatKm(clue.km)} km ${compassPoint(clue.bearing)}`;
  return answerCount > 1 ? `nearest ${where}` : where;
}
