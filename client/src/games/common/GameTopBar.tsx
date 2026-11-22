import { Button } from "../../ui";
import HomeLink from "../../components/HomeLink";
import ThemeToggle from "../../components/ThemeToggle";
import { Round } from "./Model";
import RoundProgress from "./RoundProgress";
import type { LiveProgress } from "./roundView";

export default function GameTopBar({ round, progress }: { round: Round; progress: LiveProgress }): React.JSX.Element {
  const { states, points, outcomes, score } = progress;

  // The finished game's Results button crowds the bar: on a phone the round dots then move to a row of their own
  // rather than being clipped.
  const crowded = round.gameCompleted;
  // The finished game's text is longer and has a Results button beside it: on a phone, drop the words so the bar
  // still fits the screen instead of widening the page.
  const scoreText = round.gameCompleted ? (
    <>
      <span className="top-bar__score-full">{`Final score ${score} / ${round.maxScore}`}</span>
      <span className="top-bar__score-short">{`${score} / ${round.maxScore}`}</span>
    </>
  ) : (
    `Score ${score}`
  );

  return (
    <div className={"top-bar" + (crowded ? " top-bar--wrap" : "")}>
      <div className="top-bar__start">
        <HomeLink hideTextOnMobile />
        <ThemeToggle />
      </div>
      <div className="top-bar__rounds gg-text-muted">
        {round.dailyDate !== undefined && <span className="top-bar__daily">Daily</span>}
        <span className="top-bar__round-count">
          Round {round.currentRound + 1} of {round.totalRoundCount}
        </span>
        <RoundProgress
          states={states}
          points={points}
          outcomes={outcomes}
          currentRound={round.currentRound}
          goToRound={round.goToRound}
        />
      </div>
      <div className="top-bar__end gg-text-muted">
        {scoreText}
        {round.gameCompleted && (
          <Button size="sm" variant="secondary" disabled={round.answerSave !== "saved"} onClick={round.showSummary}>
            Results
          </Button>
        )}
      </div>
    </div>
  );
}
