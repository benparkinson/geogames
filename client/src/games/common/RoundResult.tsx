import { useEffect, useRef } from "react";
import { Button, Notice, ScoreBar, Spinner } from "../../ui";
import { Round } from "./Model";
import type { RoundView } from "./roundView";
import { NextIcon } from "../../ui/icons";

/** The title of a finished round. */
function title(result: RoundView["result"]): string {
  switch (result) {
    case "outOfGuesses":
      return "Out of guesses";
    case "revealed":
      return "Answer revealed";
    default:
      return "Correct!";
  }
}

export default function RoundResult({
  view,
  round,
  animate = false,
}: {
  view: RoundView;
  round: Round;
  /** Fill the score bar cell by cell: for a round that has just ended, not one revisited. */
  animate?: boolean;
}): React.JSX.Element {
  const { result, outcome, points, breakdown } = view;

  // The points sit on the right, where the round's worth was while playing.
  return (
    <div className="round-result">
      <div
        className="round-result__banner"
        data-outcome={outcome ?? undefined}
        data-result={result === "revealed" || result === "outOfGuesses" ? result : undefined}
        role="status"
        aria-live="polite"
      >
        <div className="round-result__label">{title(result)}</div>{" "}
        <span className="round-result__points gg-mono">{`+${points}`}</span>
      </div>
      <ScoreBar className="round-result__bar" breakdown={breakdown} animate={animate} showLabels />
      {round.answerSave === "failed" ? (
        <>
          <Notice className="round-result__notice">Couldn&apos;t save your answer.</Notice>
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
              Next round <NextIcon aria-hidden="true" />
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
      className="round-result__continue"
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
