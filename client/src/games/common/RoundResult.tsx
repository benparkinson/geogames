import { Button } from "react-bootstrap";
import { ArrowRight, CheckCircleFill, EyeFill, StarFill, XCircleFill } from "react-bootstrap-icons";
import { CORRECT_ANSWER, GAVE_UP, OUT_OF_GUESSES, Round } from "./Model";
import { isPerfect, roundPoints } from "./scoring";

export default function RoundResult({ gaveUp, outOfGuesses = false, round, hintsUsed, hintCount }: {
  gaveUp: boolean;
  /** The round ended because the guesses ran out (`gaveUp` is true too). */
  outOfGuesses?: boolean;
  round: Round;
  hintsUsed: number;
  hintCount: number;
}): React.JSX.Element {
  const answerState = outOfGuesses ? OUT_OF_GUESSES : gaveUp ? GAVE_UP : CORRECT_ANSWER;
  const points = roundPoints(answerState, hintsUsed, hintCount);
  const perfect = isPerfect(answerState, hintsUsed);

  return (
    <div className="round-result mb-3">
      <div className={"mb-2 fw-semibold " + (gaveUp ? "text-secondary" : "text-success")} aria-live="polite">
        {outOfGuesses ? (
          <><XCircleFill aria-hidden="true" /> Out of guesses · 0 pts</>
        ) : gaveUp ? (
          <><EyeFill aria-hidden="true" /> Answer revealed · 0 pts</>
        ) : perfect ? (
          <><StarFill aria-hidden="true" /> Perfect! +{points}</>
        ) : (
          <><CheckCircleFill aria-hidden="true" /> Correct! +{points}</>
        )}
        {!gaveUp && !perfect && (
          <div className="small fw-normal text-muted">{hintsUsed === 1 ? "1 hint used" : `${hintsUsed} hints used`}</div>
        )}
      </div>
      {round.hasNextRound ? (
        <Button variant="primary" size="lg" className="w-100" autoFocus onClick={round.nextRound}>
          Next round <ArrowRight aria-hidden="true" />
        </Button>
      ) : (
        <Button variant="primary" size="lg" className="w-100" autoFocus onClick={round.showSummary}>
          See results
        </Button>
      )}
    </div>
  );
}
