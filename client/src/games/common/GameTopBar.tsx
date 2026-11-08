import { Button } from "react-bootstrap";
import HomeLink from "../../components/HomeLink";
import { CORRECT_ANSWER, GAVE_UP, Round, UNANSWERED } from "./Model";
import RoundProgress from "./RoundProgress";
import { roundPoints } from "./scoring";

export default function GameTopBar({ round, gameOver, gaveUp, hintsUsed }: {
  round: Round;
  gameOver: boolean;
  gaveUp: boolean;
  hintsUsed: number;
}): React.JSX.Element {
  // Reflect the local state of the current round straight away, without waiting for the refetch.
  const states = round.roundAnswerStates.slice();
  const points = round.roundPoints.slice();
  let score = round.score;
  if (gameOver) {
    const answerState = gaveUp ? GAVE_UP : CORRECT_ANSWER;
    const localPoints = roundPoints(answerState, hintsUsed, round.hintCount);
    states[round.currentRound] = answerState;
    points[round.currentRound] = localPoints;
    if (round.answerState === UNANSWERED) {
      // The round ended here and the server's copy hasn't been refetched yet.
      score += localPoints;
    }
  }

  const scoreText = round.gameCompleted
    ? `Final score ${score} / ${round.maxScore}`
    : `Score ${score}`;

  return (
    <div className="d-flex align-items-center justify-content-between px-3 py-2 gap-2">
      <HomeLink hideTextOnMobile />
      <div className="d-flex align-items-center gap-2 small text-muted overflow-hidden">
        <span className="d-none d-sm-inline text-nowrap">Round {round.currentRound + 1} of {round.totalRoundCount}</span>
        <RoundProgress states={states} points={points} currentRound={round.currentRound} goToRound={round.goToRound} />
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
