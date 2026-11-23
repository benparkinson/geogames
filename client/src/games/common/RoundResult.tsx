import { useEffect, useRef } from "react";
import { Button, Notice, Spinner } from "../../ui";
import { Round } from "./Model";
import type { RoundView } from "./roundView";
import { NextIcon, RESULT_ICONS } from "../../ui/icons";
import type { IconComponent } from "../../ui/icons";

/** What the banner says on its left: an icon, a label and an optional note under it. */
function bannerText(view: RoundView): { Icon: IconComponent; label: string; note: string | null } {
  const { result, points, hintsUsed, answersFound, answerCount } = view;
  const missed = result === "revealed" || result === "outOfGuesses";
  const partial = missed && points > 0;
  const note = partial
    ? [`${answersFound} of ${answerCount} found`, ...(hintsUsed > 0 ? [hintsUsedText(hintsUsed)] : [])].join(" · ")
    : null;
  switch (result) {
    case "outOfGuesses":
      return { Icon: RESULT_ICONS.outOfGuesses, label: "Out of guesses", note };
    case "revealed":
      return { Icon: RESULT_ICONS.revealed, label: "Answer revealed", note };
    case "firstGuess":
      return {
        Icon: RESULT_ICONS.firstGuess,
        label: answerCount > 1 ? "No wrong guesses!" : "First guess!",
        note: null,
      };
    case "hints":
      return {
        Icon: RESULT_ICONS.hints,
        label: `Correct with ${hintsUsed === 1 ? "a hint" : `${hintsUsed} hints`}`,
        note: null,
      };
    default:
      return { Icon: RESULT_ICONS.correct, label: "Correct!", note: null };
  }
}

function hintsUsedText(hintsUsed: number): string {
  return hintsUsed === 1 ? "1 hint used" : `${hintsUsed} hints used`;
}

export default function RoundResult({ view, round }: { view: RoundView; round: Round }): React.JSX.Element {
  const { result, outcome, points } = view;
  const { Icon, label, note } = bannerText(view);

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
        <div className="round-result__label">
          <div>
            <Icon aria-hidden="true" /> {label}
          </div>{" "}
          {note !== null && <div className="round-result__note gg-mono gg-text-muted">{note}</div>}
        </div>{" "}
        <span className="round-result__points gg-mono">{`+${points}`}</span>
      </div>
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
