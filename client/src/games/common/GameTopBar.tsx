import Link from "next/link";
import { Button } from "react-bootstrap";
import { HouseFill } from "react-bootstrap-icons";
import { CORRECT_ANSWER, GAVE_UP, Round } from "./Model";
import RoundProgress from "./RoundProgress";

export default function GameTopBar({ round, gameOver, gaveUp }: { round: Round; gameOver: boolean; gaveUp: boolean }): JSX.Element {
  const scoreText = round.gameCompleted
    ? `Final score ${round.score} / ${round.totalRoundCount}`
    : `Score ${round.score}`;

  // Reflect the local state of the current round straight away, without waiting for the refetch.
  const states = round.roundAnswerStates.slice();
  if (gameOver) {
    states[round.currentRound] = gaveUp ? GAVE_UP : CORRECT_ANSWER;
  }

  return (
    <div className="d-flex align-items-center justify-content-between px-3 py-2 gap-2">
      <Link href="/" aria-label="Home" className="text-decoration-none text-dark fw-semibold text-nowrap">
        <HouseFill aria-hidden="true" /> <span className="d-none d-sm-inline">Geogames</span>
      </Link>
      <div className="d-flex align-items-center gap-2 small text-muted overflow-hidden">
        <span className="d-none d-sm-inline text-nowrap">Round {round.currentRound + 1} of {round.totalRoundCount}</span>
        <RoundProgress states={states} currentRound={round.currentRound} goToRound={round.goToRound} />
      </div>
      <div className="d-flex align-items-center gap-2 small text-muted text-nowrap">
        {scoreText}
        {round.gameCompleted && (
          <Button size="sm" variant="outline-primary" onClick={round.showSummary}>Results</Button>
        )}
      </div>
    </div>
  );
}
