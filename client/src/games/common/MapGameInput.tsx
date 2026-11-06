import { GuessBox } from "../../components/GuessBox";
import { Round } from "./Model";
import RoundResult from "./RoundResult";
import { AnswerModel, GuessResult, HintConfig } from "./MapGame";
import { Button } from "react-bootstrap";
import { useEffect, useRef, useState } from "react";
import { EyeFill, Lightbulb, XLg } from "react-bootstrap-icons";

function prefersPreciseInput(): boolean {
  return typeof window !== "undefined" && window.matchMedia("(pointer: fine)").matches;
}

function MapGameInput({
  gameOver,
  gaveUp,
  round,
  wrongGuesses,
  submitGuess,
  useHint,
  hintsUsed,
  hints,
  giveUp,
  inputPlaceholder,
  suggestions
}: MapGameInputProps): JSX.Element {
  const [guess, setGuess] = useState("");
  const [feedback, setFeedback] = useState<string | null>(null);
  const [shakeCount, setShakeCount] = useState(0);
  const [confirmReveal, setConfirmReveal] = useState(false);
  const revealTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    return () => {
      if (revealTimer.current) {
        clearTimeout(revealTimer.current);
      }
    };
  }, []);

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const result = submitGuess(guess);

    switch (result.kind) {
      case "empty":
        setFeedback("Type a guess first.");
        break;
      case "duplicate":
        setFeedback(`You've already guessed ${result.guess}.`);
        break;
      case "unknown":
        setFeedback(`We don't recognise "${result.guess}" as a country. Check the spelling.`);
        setShakeCount(shakeCount + 1);
        break;
      case "wrong":
        setFeedback(`"${result.guess}" isn't right. Try again.`);
        setGuess("");
        setShakeCount(shakeCount + 1);
        break;
      case "correct":
        setFeedback(result.remaining > 0 ? `Correct! ${result.remaining} to go.` : "Correct!");
        setGuess("");
        break;
      case "over":
        break;
    }
  }

  function handleRevealClick() {
    if (!confirmReveal) {
      setConfirmReveal(true);
      revealTimer.current = setTimeout(() => setConfirmReveal(false), 3000);
      return;
    }
    if (revealTimer.current) {
      clearTimeout(revealTimer.current);
    }
    setConfirmReveal(false);
    giveUp();
  }

  const hintsLeft = hints ? hints.total - hintsUsed : 0;
  const hintTitle = hints ? (hintsLeft > 0 ? hints.description : "No hints left") : "";

  return (
    <div>
      {gameOver ? (
        <RoundResult gaveUp={gaveUp} round={round} />
      ) : (
        <>
        <form onSubmit={handleSubmit} className="d-flex gap-2 mb-2">
          <div className="flex-grow-1">
            <GuessBox
              key={shakeCount}
              className={shakeCount > 0 ? "shake" : undefined}
              autoFocus={shakeCount > 0 || prefersPreciseInput()}
              value={guess}
              name={inputPlaceholder}
              onChange={e => setGuess(e.currentTarget.value)}
              disabled={gameOver}
            />
          </div>
          <Button type="submit" variant="primary" disabled={gameOver}>Guess</Button>
        </form>

        <div className="d-flex flex-wrap gap-2 mb-2">
          {hints && (
            <span title={hintTitle}>
              <Button variant="outline-warning" onClick={useHint} disabled={gameOver || hintsLeft <= 0}>
                <Lightbulb aria-hidden="true" /> Hint ({hintsLeft} left)
              </Button>
            </span>
          )}
          <Button variant="outline-danger" onClick={handleRevealClick} disabled={gameOver}>
            <EyeFill aria-hidden="true" /> {confirmReveal ? "Tap again to reveal" : "Reveal answer"}
          </Button>
        </div>
        </>
      )}

      <div className="guess-feedback small" aria-live="polite">{feedback}</div>

      {wrongGuesses.length > 0 && (
        <div>
          <div className="text-muted" style={{ fontSize: "0.7rem" }}>Wrong guesses</div>
          <div className="d-flex flex-wrap gap-1">
            {wrongGuesses.map((a, i) => (
              <span key={i} className="badge bg-body-secondary text-body-secondary fw-normal text-decoration-line-through border">
                <XLg aria-hidden="true" /> {a.answer}
              </span>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

export class MapGameInputProps {
  gameOver: boolean;
  gaveUp: boolean;
  round: Round;
  wrongGuesses: AnswerModel[];
  submitGuess: (guess: string) => GuessResult;
  useHint: () => void;
  hintsUsed: number;
  hints?: HintConfig;
  giveUp: () => void;
  inputPlaceholder: string;
  suggestions?: string[];
}

export default MapGameInput;
