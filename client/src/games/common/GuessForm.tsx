import { useRef, useState } from "react";
import { Button } from "../../ui";
import { GuessBox } from "./GuessBox";
import { feedbackText } from "./guessFeedback";
import { GuessResult } from "./guessReducer";
import type { Suggestion } from "./suggestions";

function prefersPreciseInput(): boolean {
  return typeof window !== "undefined" && window.matchMedia("(pointer: fine)").matches;
}

/** Alternates between two classes with the same animation, so each wrong guess replays the shake. */
function shakeClass(shakeCount: number): string | undefined {
  if (shakeCount === 0) return undefined;
  return shakeCount % 2 === 1 ? "shake" : "shake-again";
}

/** The guess box and its button, and what the last guess earned. Only shown while the round is being played. */
export default function GuessForm({
  submitGuess,
  inputPlaceholder,
  answerNoun,
  suggestions,
}: {
  submitGuess: (guess: string) => GuessResult;
  inputPlaceholder: string;
  answerNoun: string;
  suggestions?: Suggestion[];
}): React.JSX.Element {
  const [guess, setGuess] = useState("");
  const [result, setResult] = useState<GuessResult | null>(null);
  const [shakeCount, setShakeCount] = useState(0);
  const inputRef = useRef<HTMLInputElement>(null);

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
    // Keep typing in the box after a guess, whichever way it was sent. Called
    // during the submit, so a phone keeps (or reopens) its keyboard.
    inputRef.current?.focus();
  }

  // Worked out from the result on every render.
  const feedback = result ? feedbackText(result, answerNoun) : null;

  return (
    <>
      <form onSubmit={handleSubmit} className="d-flex gap-2 mb-2">
        <div className="flex-grow-1">
          <GuessBox
            ref={inputRef}
            className={shakeClass(shakeCount)}
            autoFocus={prefersPreciseInput()}
            value={guess}
            name={inputPlaceholder}
            onChange={setGuess}
            suggestions={suggestions}
          />
        </div>
        {/* Pressing the button would take the focus from the box and close a phone's keyboard. */}
        <Button type="submit" variant="primary" onMouseDown={(e) => e.preventDefault()}>
          Guess
        </Button>
      </form>

      <div className="guess-feedback small" aria-live="polite">
        {feedback}
      </div>
    </>
  );
}
