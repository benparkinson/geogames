import { Button } from "../../ui";
import HomeLink from "../../components/HomeLink";
import ThemeToggle from "../../components/ThemeToggle";
import { finalAnswerState, Round, UNANSWERED } from "./Model";
import { roundOutcome } from "./outcome";
import RoundProgress from "./RoundProgress";
import { roundPoints } from "./scoring";

export default function GameTopBar({
  round,
  gameOver,
  gaveUp,
  outOfGuesses = false,
  hintsUsed,
  wrongGuesses = 0,
  answersFound = 0,
  answerCount = 1,
}: {
  round: Round;
  gameOver: boolean;
  gaveUp: boolean;
  /** The round ended because the guesses ran out (`gaveUp` is true too). */
  outOfGuesses?: boolean;
  hintsUsed: number;
  /** Wrong guesses made this round: none and no hints makes a won round a first guess. */
  wrongGuesses?: number;
  /** How many of the round's answers were found, for the points of a round that was given up. */
  answersFound?: number;
  answerCount?: number;
}): React.JSX.Element {
  // Reflect the local state of the current round straight away, without waiting for the server.
  const states = round.roundAnswerStates.slice();
  const points = round.roundPoints.slice();
  const outcomes = states.map((state, i) =>
    roundOutcome(state, round.roundHintsUsed[i], round.roundFirstGuess[i], points[i]),
  );
  let score = round.score;
  if (gameOver) {
    const answerState = finalAnswerState(gaveUp, outOfGuesses);
    const localPoints = roundPoints(answerState, hintsUsed, round.hintCount, answersFound, answerCount);
    states[round.currentRound] = answerState;
    points[round.currentRound] = localPoints;
    outcomes[round.currentRound] = roundOutcome(answerState, hintsUsed, wrongGuesses === 0, localPoints);
    if (round.answerState === UNANSWERED) {
      // The round ended here and the server's copy hasn't been updated yet.
      score += localPoints;
    }
  }

  // The finished game's Results button crowds the bar: on a phone the round dots then move to a row of their own
  // rather than being clipped.
  const crowded = round.gameCompleted;
  // The finished game's text is longer and has a Results button beside it: on a phone, drop the words so the bar
  // still fits the screen instead of widening the page.
  const scoreText = round.gameCompleted ? (
    <>
      <span className="d-none d-sm-inline">{`Final score ${score} / ${round.maxScore}`}</span>
      <span className="d-sm-none">{`${score} / ${round.maxScore}`}</span>
    </>
  ) : (
    `Score ${score}`
  );

  return (
    <div
      className={
        "d-flex align-items-center justify-content-between px-3 py-2 gap-2" + (crowded ? " top-bar--wrap" : "")
      }
    >
      <div className="d-flex align-items-center gap-2">
        <HomeLink hideTextOnMobile />
        <ThemeToggle />
      </div>
      <div className="top-bar__rounds d-flex align-items-center gap-2 small gg-text-muted overflow-hidden">
        {round.dailyDate !== undefined && <span className="fw-semibold text-nowrap">Daily</span>}
        <span className="d-none d-sm-inline text-nowrap">
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
      <div className="d-flex align-items-center gap-2 small gg-text-muted text-nowrap">
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
