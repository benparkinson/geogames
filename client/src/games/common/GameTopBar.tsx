import { Button } from "../../ui";
import HomeLink from "../../components/HomeLink";
import { Round } from "./Model";
import RoundProgress from "./RoundProgress";
import { DIFFICULTY_LABEL } from "./difficultyLabel";
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

  // Not the round's game: a mixed game's rounds differ, and the client can't tell a mixed game from its rounds alone.
  const title = round.dailyDate !== undefined ? "Daily" : "Practice";
  const roundNumber = String(round.currentRound + 1).padStart(2, "0");
  const meta = [
    `round ${roundNumber} of ${String(round.totalRoundCount).padStart(2, "0")}`,
    ...DIFFICULTY_LABEL[round.difficulty],
  ];

  return (
    <div className={"top-bar" + (crowded ? " top-bar--wrap" : "")}>
      <div className="top-bar__start">
        <HomeLink hideTextOnMobile />
      </div>
      <div className="top-bar__title">
        <span className="top-bar__game">{title}</span>
        <span className="top-bar__meta">{meta.join(" · ")}</span>
      </div>
      <div className="top-bar__end">
        {scoreText}
        {round.gameCompleted && (
          <Button size="sm" variant="secondary" disabled={round.answerSave !== "saved"} onClick={round.showSummary}>
            Results
          </Button>
        )}
      </div>
      <div className="top-bar__rounds">
        <RoundProgress
          states={states}
          points={points}
          outcomes={outcomes}
          currentRound={round.currentRound}
          goToRound={round.goToRound}
        />
      </div>
    </div>
  );
}
