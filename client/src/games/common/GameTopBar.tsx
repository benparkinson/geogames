import Link from "next/link";
import { Button } from "react-bootstrap";
import { ChevronLeft, ChevronRight, HouseFill } from "react-bootstrap-icons";
import { Round } from "./Model";

export default function GameTopBar({ round, gameOver }: { round: Round; gameOver: boolean }): JSX.Element {
  const nextDisabled = !round.hasNextRound || !gameOver;
  const scoreText = round.gameCompleted
    ? `Final score ${round.score} / ${round.totalRoundCount}`
    : `Score ${round.score}`;

  return (
    <div className="d-flex align-items-center justify-content-between px-3 py-2 gap-2">
      <Link href="/" aria-label="Home" className="text-decoration-none text-dark fw-semibold text-nowrap">
        <HouseFill aria-hidden="true" /> <span className="d-none d-sm-inline">Geogames</span>
      </Link>
      <div className="small text-muted text-nowrap">
        Round {round.currentRound + 1} of {round.totalRoundCount} · {scoreText}
      </div>
      <div className="d-flex gap-2">
        <Button size="sm" variant="outline-secondary" onClick={round.prevRound} disabled={!round.hasPreviousRound} aria-label="Previous round">
          <ChevronLeft aria-hidden="true" /> <span className="d-none d-sm-inline">Previous</span>
        </Button>
        <span title={!gameOver && round.hasNextRound ? "Answer or reveal this round first" : undefined}>
          <Button size="sm" variant="outline-secondary" onClick={round.nextRound} disabled={nextDisabled} aria-label="Next round">
            <span className="d-none d-sm-inline">Next round</span> <ChevronRight aria-hidden="true" />
          </Button>
        </span>
      </div>
    </div>
  );
}
