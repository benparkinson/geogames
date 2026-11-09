import { GuessBox } from "../../components/GuessBox";
import { CORRECT_ANSWER, Round } from "./Model";
import { roundPoints } from "./scoring";
import RoundResult from "./RoundResult";
import GuessesLeft from "./GuessesLeft";
import { HintConfig } from "./MapGame";
import { Guess, GuessResult } from "./guessReducer";
import { Button } from "../../ui";
import { useEffect, useRef, useState } from "react";
import { EyeFill, Lightbulb, XLg } from "react-bootstrap-icons";

function prefersPreciseInput(): boolean {
  return typeof window !== "undefined" && window.matchMedia("(pointer: fine)").matches;
}

function MapGameInput({
  gameOver,
  gaveUp,
  outOfGuesses,
  guessesLeft,
  round,
  wrongGuesses,
  submitGuess,
  onHint,
  hintsUsed,
  hintCount,
  hints,
  giveUp,
  inputPlaceholder,
  answerNoun,
  suggestions
}: MapGameInputProps): React.JSX.Element {
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
        setFeedback(`We don't recognise "${result.guess}" as a ${answerNoun}. Check the spelling.`);
        setShakeCount(shakeCount + 1);
        break;
      case "wrong":
        setFeedback(
          result.guessesLeft === 1
            ? `"${result.guess}" isn't right. Last guess!`
            : Number.isFinite(result.guessesLeft)
              ? `"${result.guess}" isn't right. ${result.guessesLeft} guesses left.`
              : `"${result.guess}" isn't right. Try again.`,
        );
        setGuess("");
        setShakeCount(shakeCount + 1);
        break;
      case "outOfGuesses":
        setFeedback(`"${result.guess}" isn't right. You're out of guesses.`);
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

  const hintsLeft = hints ? hintCount - hintsUsed : 0;
  // The drop the next hint causes, as a difference so it rounds the same way as the score.
  const nextHintCost =
    roundPoints(CORRECT_ANSWER, hintsUsed, hintCount) - roundPoints(CORRECT_ANSWER, hintsUsed + 1, hintCount);
  const hintTitle = hints ? (hintsLeft > 0 ? `${hints.description} (−${nextHintCost} pts)` : "No hints left") : "";

  return (
    <div>
      {gameOver ? (
        <RoundResult gaveUp={gaveUp} outOfGuesses={outOfGuesses} round={round} hintsUsed={hintsUsed} hintCount={hintCount} />
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
              suggestions={suggestions}
            />
          </div>
          <Button type="submit" variant="primary" disabled={gameOver}>Guess</Button>
        </form>

        <div className="d-flex flex-wrap gap-2 mb-2">
          {hints && (
            <span title={hintTitle}>
              <Button variant="hint" onClick={onHint} disabled={gameOver || hintsLeft <= 0}>
                <Lightbulb aria-hidden="true" /> Hint ({hintsLeft} left)
              </Button>
            </span>
          )}
          <Button variant="danger" onClick={handleRevealClick} disabled={gameOver}>
            <EyeFill aria-hidden="true" /> {confirmReveal ? "Tap again to reveal" : "Reveal answer"}
          </Button>
          <span className="small text-muted align-self-center">
            {`Worth ${roundPoints(CORRECT_ANSWER, hintsUsed, hintCount)} pts`}
          </span>
        </div>
        </>
      )}

      <div className="guess-feedback small" aria-live="polite">{feedback}</div>

      {!gameOver && <GuessesLeft left={guessesLeft} limit={round.guessLimit} />}

      {wrongGuesses.length > 0 && (
        <div>
          <div className="text-muted wrong-guesses-label">Wrong guesses</div>
          <div className="d-flex flex-wrap gap-1">
            {wrongGuesses.map((a, i) => (
              <span key={i} className="badge bg-body-secondary text-body-secondary fw-normal text-decoration-line-through border">
                <XLg aria-hidden="true" /> {a.text}
              </span>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

interface MapGameInputProps {
  gameOver: boolean;
  gaveUp: boolean;
  /** The round ended because the guesses ran out (`gaveUp` is true too). */
  outOfGuesses: boolean;
  /** Wrong guesses left; Infinity when the round has no limit. */
  guessesLeft: number;
  round: Round;
  wrongGuesses: Guess[];
  submitGuess: (guess: string) => GuessResult;
  onHint: () => void;
  hintsUsed: number;
  hintCount: number;
  hints?: HintConfig;
  giveUp: () => void;
  inputPlaceholder: string;
  answerNoun: string;
  suggestions?: string[];
}

export default MapGameInput;
