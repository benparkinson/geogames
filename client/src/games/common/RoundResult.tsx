import { Button, Notice } from "../../ui";
import { ArrowRight, CheckCircleFill, EyeFill, StarFill, XCircleFill } from "react-bootstrap-icons";
import { finalAnswerState, isPartlyCorrect, Round } from "./Model";
import { isPerfect, roundPoints } from "./scoring";

export default function RoundResult({
  gaveUp,
  outOfGuesses = false,
  round,
  hintsUsed,
  hintCount,
  answersFound = 0,
  answerCount = 1,
}: {
  gaveUp: boolean;
  /** The round ended because the guesses ran out (`gaveUp` is true too). */
  outOfGuesses?: boolean;
  round: Round;
  hintsUsed: number;
  hintCount: number;
  /** How many of the round's answers were found, and how many it has: a revealed round scores their share. */
  answersFound?: number;
  answerCount?: number;
}): React.JSX.Element {
  const answerState = finalAnswerState(gaveUp, outOfGuesses);
  const points = roundPoints(answerState, hintsUsed, hintCount, answersFound, answerCount);
  const perfect = isPerfect(answerState, hintsUsed);
  const partial = isPartlyCorrect(answerState, points);
  const found = `${answersFound} of ${answerCount} found`;

  return (
    <div className="round-result mb-3">
      <div
        className={"mb-2 fw-semibold " + (gaveUp && !partial ? "gg-text-revealed" : "gg-text-correct")}
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
        ) : perfect ? (
          <>
            <StarFill aria-hidden="true" /> Perfect! +{points}
          </>
        ) : (
          <>
            <CheckCircleFill aria-hidden="true" /> Correct! +{points}
          </>
        )}
        {(!gaveUp || partial) && !perfect && (
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
      ) : round.hasNextRound ? (
        <Button
          variant="primary"
          size="lg"
          block
          autoFocus
          disabled={round.answerSave === "saving"}
          onClick={round.nextRound}
        >
          Next round <ArrowRight aria-hidden="true" />
        </Button>
      ) : (
        <Button
          variant="primary"
          size="lg"
          block
          autoFocus
          disabled={round.answerSave === "saving"}
          onClick={round.showSummary}
        >
          See results
        </Button>
      )}
    </div>
  );
}
