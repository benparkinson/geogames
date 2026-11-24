import { Button } from "../../ui";
import { NextIcon } from "../../ui/icons";
import SiteTitle from "../../components/SiteTitle";
import { Round } from "./Model";
import RoundProgress from "./RoundProgress";
import { DIFFICULTY_LABEL } from "./difficultyLabel";
import type { LiveProgress } from "./roundView";

export default function GameTopBar({ round, progress }: { round: Round; progress: LiveProgress }): React.JSX.Element {
  const { states, points, outcomes, score } = progress;

  // Not the round's game: a mixed game's rounds differ, and the client can't tell a mixed game from its rounds alone.
  const title = round.dailyDate !== undefined ? "Daily" : "Practice";
  const { Icon, title: kindTitle } = round.kind;
  const difficulty = DIFFICULTY_LABEL[round.difficulty];

  // Two columns of two lines that line up: the title over the round's name, and the score over its label (the
  // Results button once the game is complete).
  return (
    <div className="top-bar">
      <div className="top-bar__start">
        <SiteTitle collapsible />
      </div>
      <div className="top-bar__title">
        <span className="top-bar__game">{title}</span>
        <span className="top-bar__meta">
          <span className="top-bar__kind">
            <Icon aria-hidden="true" /> {kindTitle}
          </span>
          {difficulty.map((label) => (
            <span key={label} className="top-bar__difficulty">{` · ${label}`}</span>
          ))}
        </span>
      </div>
      {round.gameCompleted ? (
        <div className="top-bar__end" role="group" aria-label={`Final score ${score} of ${round.maxScore}`}>
          <span className="top-bar__score">{`${score} / ${round.maxScore}`}</span>
          <Button
            className="top-bar__results"
            size="sm"
            variant="secondary"
            disabled={round.answerSave !== "saved"}
            onClick={round.showSummary}
          >
            Results <NextIcon aria-hidden="true" />
          </Button>
        </div>
      ) : (
        <div className="top-bar__end" role="group" aria-label={`Score ${score}`}>
          <span className="top-bar__score">{score}</span>
          <span className="top-bar__score-label">score</span>
        </div>
      )}
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
