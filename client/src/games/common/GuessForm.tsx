import { useState } from "react";
import { Button } from "../../ui";
import { GuessBox } from "./GuessBox";
import { feedbackText } from "./guessFeedback";
import { GuessResult } from "./guessReducer";

function prefersPreciseInput(): boolean {
  return typeof window !== "undefined" && window.matchMedia("(pointer: fine)").matches;
}

/** The guess box and its button, and what the last guess earned. Only shown while the round is being played. */
export default function GuessForm({
  submitGuess,
  inputPlaceholder,
  answerNoun,
  answerCount,
  showClues,
  suggestions,
}: {
  submitGuess: (guess: string) => GuessResult;
  inputPlaceholder: string;
  answerNoun: string;
  /** How many answers the round has: with several, a clue points to the nearest one still to find. */
  answerCount: number;
  /** Whether to show distance clues (false in Hard mode). */
  showClues: boolean;
  suggestions?: string[];
}): React.JSX.Element {
  const [guess, setGuess] = useState("");
  const [result, setResult] = useState<GuessResult | null>(null);
  const [shakeCount, setShakeCount] = useState(0);

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const outcome = submitGuess(guess);
    setResult(outcome);

    switch (outcome.kind) {
      case "unknown":
        setShakeCount(shakeCount + 1);
        break;
      case "wrong":
      case "outOfGuesses":
        setGuess("");
        setShakeCount(shakeCount + 1);
        break;
      case "correct":
        setGuess("");
        break;
    }
  }

  // Worked out from the result on every render, so switching Hard mode changes it straight away.
  const feedback = result ? feedbackText(result, answerNoun, answerCount, showClues) : null;

  return (
    <>
      <form onSubmit={handleSubmit} className="d-flex gap-2 mb-2">
        <div className="flex-grow-1">
          <GuessBox
            key={shakeCount}
            className={shakeCount > 0 ? "shake" : undefined}
            autoFocus={shakeCount > 0 || prefersPreciseInput()}
            value={guess}
            name={inputPlaceholder}
            onChange={(e) => setGuess(e.currentTarget.value)}
            suggestions={suggestions}
          />
        </div>
        <Button type="submit" variant="primary">
          Guess
        </Button>
      </form>

      <div className="guess-feedback small" aria-live="polite">
        {feedback}
      </div>
    </>
  );
}
