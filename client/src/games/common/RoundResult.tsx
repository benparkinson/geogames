import { useEffect, useRef } from "react";
import { Button, Notice, Spinner } from "../../ui";
import { ArrowRight, CheckCircleFill, EyeFill, StarFill, XCircleFill } from "react-bootstrap-icons";
import { finalAnswerState, Round } from "./Model";
import { roundOutcome, RoundOutcome } from "./outcome";
import { roundPoints } from "./scoring";

export default function RoundResult({
  gaveUp,
  outOfGuesses = false,
  round,
  hintsUsed,
  hintCount,
  wrongGuesses = 0,
  answersFound = 0,
  answerCount = 1,
}: {
  gaveUp: boolean;
  /** The round ended because the guesses ran out (`gaveUp` is true too). */
  outOfGuesses?: boolean;
  round: Round;
  hintsUsed: number;
  hintCount: number;
  /** Wrong guesses made this round: none and no hints makes a won round a first guess. */
  wrongGuesses?: number;
  /** How many of the round's answers were found, and how many it has: a revealed round scores their share. */
  answersFound?: number;
  answerCount?: number;
}): React.JSX.Element {
  const answerState = finalAnswerState(gaveUp, outOfGuesses);
  const points = roundPoints(answerState, hintsUsed, hintCount, answersFound, answerCount);
  const outcome = roundOutcome(answerState, hintsUsed, wrongGuesses === 0, points) as RoundOutcome;
  const partial = gaveUp && points > 0;
  const found = `${answersFound} of ${answerCount} found`;

  return (
    <div className="round-result mb-3">
      <div
        className="mb-2 fw-semibold"
        data-outcome={outcome}
        data-result={outOfGuesses ? "outOfGuesses" : gaveUp ? "revealed" : undefined}
        aria-live="polite"
      >
        {outOfGuesses ? (
          <>
            <XCircleFill aria-hidden="true" /> Out of guesses · {partial ? `${found} · +${points}` : "0 pts"}
          </>
        ) : gaveUp ? (
          <>
            <EyeFill aria-hidden="true" /> Answer revealed · {partial ? `${found} · +${points}` : "0 pts"}
          </>
        ) : outcome === "firstGuess" ? (
          <>
            <StarFill aria-hidden="true" /> {answerCount > 1 ? "No wrong guesses!" : "First guess!"} +{points}
          </>
        ) : outcome === "hints" ? (
          <>
            <CheckCircleFill aria-hidden="true" /> Correct with {hintsUsed === 1 ? "a hint" : `${hintsUsed} hints`} · +
            {points}
          </>
        ) : (
          <>
            <CheckCircleFill aria-hidden="true" /> Correct! +{points}
          </>
        )}
        {partial && hintsUsed > 0 && (
          <div className="small fw-normal gg-text-muted">
            {hintsUsed === 1 ? "1 hint used" : `${hintsUsed} hints used`}
          </div>
        )}
      </div>
      {round.answerSave === "failed" ? (
        <>
          <Notice className="mb-2">Couldn&apos;t save your answer.</Notice>
          <Button variant="primary" size="lg" block autoFocus onClick={round.retryAnswer}>
            Try again
          </Button>
        </>
      ) : (
        <ContinueButton
          saving={round.answerSave === "saving"}
          onClick={round.hasNextRound ? round.nextRound : round.showSummary}
        >
          {round.hasNextRound ? (
            <>
              Next round <ArrowRight aria-hidden="true" />
            </>
          ) : (
            "See results"
          )}
        </ContinueButton>
      )}
    </div>
  );
}

/** The button that leaves the round. It shows a spinner while the answer is saving, and takes focus once it is ready, so Enter works again. */
function ContinueButton({
  saving,
  onClick,
  children,
}: {
  saving: boolean;
  onClick: () => void;
  children: React.ReactNode;
}): React.JSX.Element {
  const ref = useRef<HTMLButtonElement>(null);
  useEffect(() => {
    if (!saving) {
      ref.current?.focus();
    }
  }, [saving]);
  return (
    <Button
      ref={ref}
      variant="primary"
      size="lg"
      block
      className="position-relative"
      disabled={saving}
      aria-busy={saving}
      onClick={onClick}
    >
      <span className={saving ? "gg-btn__label--hidden" : undefined}>{children}</span>
      {saving && (
        <span className="gg-btn__overlay">
          <Spinner size="sm" />
        </span>
      )}
    </Button>
  );
}
