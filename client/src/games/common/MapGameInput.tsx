import { GuessBox } from "../../components/GuessBox";
import { CORRECT_ANSWER, Round } from "./Model";
import { roundPoints } from "./scoring";
import RoundResult from "./RoundResult";
import RoundStatus from "./RoundStatus";
import { HintConfig } from "./MapGame";
import { Guess, GuessClue, GuessResult } from "./guessReducer";
import { compassPoint, formatKm } from "./distance";
import { Button, Chip } from "../../ui";
import { useEffect, useRef, useState } from "react";
import { ArrowUp, EyeFill, Lightbulb, XLg } from "react-bootstrap-icons";

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
  answerCount,
  showClues,
  suggestions,
}: MapGameInputProps): React.JSX.Element {
  const [guess, setGuess] = useState("");
  const [result, setResult] = useState<GuessResult | null>(null);
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
        <RoundResult
          gaveUp={gaveUp}
          outOfGuesses={outOfGuesses}
          round={round}
          hintsUsed={hintsUsed}
          hintCount={hintCount}
        />
      ) : (
        <>
          <RoundStatus
            guessesLeft={guessesLeft}
            guessLimit={round.guessLimit}
            points={roundPoints(CORRECT_ANSWER, hintsUsed, hintCount)}
          />

          <form onSubmit={handleSubmit} className="d-flex gap-2 mb-2">
            <div className="flex-grow-1">
              <GuessBox
                key={shakeCount}
                className={shakeCount > 0 ? "shake" : undefined}
                autoFocus={shakeCount > 0 || prefersPreciseInput()}
                value={guess}
                name={inputPlaceholder}
                onChange={(e) => setGuess(e.currentTarget.value)}
                disabled={gameOver}
                suggestions={suggestions}
              />
            </div>
            <Button type="submit" variant="primary" disabled={gameOver}>
              Guess
            </Button>
          </form>

          <div className="guess-feedback small" aria-live="polite">
            {feedback}
          </div>

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
          </div>
        </>
      )}

      {wrongGuesses.length > 0 && (
        <div>
          <div className="gg-text-muted wrong-guesses-label">Wrong guesses</div>
          <div className="d-flex flex-wrap gap-1">
            {wrongGuesses.map((a, i) => (
              <Chip key={i} tone="neutral" className="guess-chip">
                <XLg aria-hidden="true" className="guess-chip__x" /> {a.text}
                {showClues && a.clue && <ClueBadge clue={a.clue} />}
              </Chip>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

function feedbackText(result: GuessResult, answerNoun: string, answerCount: number, showClues: boolean): string | null {
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
function clueText(clue: GuessClue, answerCount: number): string {
  const where = `${formatKm(clue.km)} km ${compassPoint(clue.bearing)}`;
  return answerCount > 1 ? `nearest ${where}` : where;
}

/** The distance and an arrow towards the answer, after a wrong guess's name. */
function ClueBadge({ clue }: { clue: GuessClue }): React.JSX.Element {
  return (
    <span className="ms-2 fw-semibold">
      {formatKm(clue.km)} km <ArrowUp aria-hidden="true" style={{ transform: `rotate(${clue.bearing}deg)` }} />
      <span className="gg-visually-hidden">{` ${formatKm(clue.km)} km ${compassPoint(clue.bearing)}`}</span>
    </span>
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
  /** How many answers the round has: with several, a clue points to the nearest one still to find. */
  answerCount: number;
  /** Whether to show distance clues (false in Hard mode). */
  showClues: boolean;
  suggestions?: string[];
}

export default MapGameInput;
