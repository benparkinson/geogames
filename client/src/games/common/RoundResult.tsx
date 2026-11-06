import { Button } from "react-bootstrap";
import { ArrowRight, CheckCircleFill, EyeFill } from "react-bootstrap-icons";
import { Round } from "./Model";

export default function RoundResult({ gaveUp, round }: { gaveUp: boolean; round: Round }): JSX.Element {
  return (
    <div className="round-result mb-3">
      <div className={"mb-2 fw-semibold " + (gaveUp ? "text-secondary" : "text-success")} aria-live="polite">
        {gaveUp ? (
          <><EyeFill aria-hidden="true" /> Answer revealed</>
        ) : (
          <><CheckCircleFill aria-hidden="true" /> Correct!</>
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
