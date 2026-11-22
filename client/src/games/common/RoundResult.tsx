import { useEffect, useRef } from "react";
import { Button, Notice, Spinner } from "../../ui";
import { Round } from "./Model";
import type { RoundView } from "./roundView";
import { NextIcon, RESULT_ICONS } from "../../ui/icons";

export default function RoundResult({ view, round }: { view: RoundView; round: Round }): React.JSX.Element {
  const { result, outcome, points, hintsUsed, answersFound, answerCount } = view;
  const partial = (result === "revealed" || result === "outOfGuesses") && points > 0;
  const found = `${answersFound} of ${answerCount} found`;

  return (
    <div className="round-result mb-3">
      <div
        className="mb-2 fw-semibold"
        data-outcome={outcome ?? undefined}
        data-result={result === "revealed" || result === "outOfGuesses" ? result : undefined}
        aria-live="polite"
      >
        {result === "outOfGuesses" ? (
          <>
            <RESULT_ICONS.outOfGuesses aria-hidden="true" /> Out of guesses ·{" "}
            {partial ? `${found} · +${points}` : "0 pts"}
          </>
        ) : result === "revealed" ? (
          <>
            <RESULT_ICONS.revealed aria-hidden="true" /> Answer revealed · {partial ? `${found} · +${points}` : "0 pts"}
          </>
        ) : result === "firstGuess" ? (
          <>
            <RESULT_ICONS.firstGuess aria-hidden="true" /> {answerCount > 1 ? "No wrong guesses!" : "First guess!"} +
            {points}
          </>
        ) : result === "hints" ? (
          <>
            <RESULT_ICONS.hints aria-hidden="true" /> Correct with {hintsUsed === 1 ? "a hint" : `${hintsUsed} hints`} ·
            +{points}
          </>
        ) : (
          <>
            <RESULT_ICONS.correct aria-hidden="true" /> Correct! +{points}
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
