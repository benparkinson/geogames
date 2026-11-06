import { GuessBox } from "../../components/GuessBox";
import { Round } from "./Model";
import { AnswerModel, GuessResult, HintConfig } from "./MapGame";
import { Button } from "react-bootstrap";
import { useEffect, useRef, useState } from "react";
import { ChevronLeft, ChevronRight, EyeFill, Lightbulb } from "react-bootstrap-icons";
import Answer from "./Answer";

function MapGameInput({
  gameOver,
  round,
  answers,
  submitGuess,
  useHint,
  hintsUsed,
  hints,
  giveUp,
  inputPlaceholder
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

  function renderAnswers(): JSX.Element[] {
    return answers.map((answer, index) => (
      <div className="m-1" key={index}>
        <Answer answer={answer.answer} isCorrect={answer.correct} />
      </div>
    ));
  }

  function nextRoundDisabled(): boolean {
    if (!round.hasNextRound) {
      return true;
    }
    return !gameOver;
  }

  const hintsLeft = hints ? hints.total - hintsUsed : 0;
  const hintTitle = hints ? (hintsLeft > 0 ? hints.description : "No hints left") : "";

  return (
    <div id="input" className="m-1">
      <form onSubmit={handleSubmit} className="d-flex gap-2 mb-2">
        <div className="flex-grow-1">
          <GuessBox
            key={shakeCount}
            className={shakeCount > 0 ? "shake" : undefined}
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
        <div className="d-flex gap-2 ms-auto">
          <Button
            variant="outline-secondary"
            onClick={round.prevRound}
            disabled={!round.hasPreviousRound}
            aria-label="Previous round"
          >
            <ChevronLeft aria-hidden="true" /> <span className="d-none d-sm-inline">Previous</span>
          </Button>
          <span title={!gameOver && round.hasNextRound ? "Answer or reveal this round first" : undefined}>
            <Button
              variant="outline-secondary"
              onClick={round.nextRound}
              disabled={nextRoundDisabled()}
              aria-label="Next round"
            >
              <span className="d-none d-sm-inline">Next round</span> <ChevronRight aria-hidden="true" />
            </Button>
          </span>
        </div>
      </div>

      <div className="guess-feedback small" aria-live="polite">{feedback}</div>

      <div className="d-flex flex-wrap">
        {renderAnswers()}
      </div>
    </div>
  );
}

export class MapGameInputProps {
  gameOver: boolean;
  round: Round;
  answers: AnswerModel[];
  submitGuess: (guess: string) => GuessResult;
  useHint: () => void;
  hintsUsed: number;
  hints?: HintConfig;
  giveUp: () => void;
  inputPlaceholder: string;
}

export default MapGameInput;
