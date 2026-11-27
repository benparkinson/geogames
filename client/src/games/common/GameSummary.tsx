import Link from "next/link";
import { Button, buttonClass, ScoreKey, Spinner } from "../../ui";
import KofiBadge from "../../components/KofiBadge";
import LoadingSpinner from "../../components/LoadingSpinner";
import { useQueries } from "@tanstack/react-query";
import { roundQuery, useCreateGame } from "../../api/games";
import { creationErrorMessage } from "../../api/http";
import { RANDOM_OPTION } from "../registry";
import { GAME_NAMES } from "../names";
import { summariseGame } from "./summaryView";
import { DIFFICULTY_LABEL } from "./difficultyLabel";
import DailyComparison from "../../daily/DailyComparison";
import ShareButton from "../../daily/ShareButton";
import { type DailyInfo } from "../../daily/dailyStorage";
import { useDailyShareText } from "../../daily/useDailyShareText";
import PageShell from "../../components/PageShell";
import SummaryRow from "./SummaryRow";
import ChallengeShare from "../../challenge/ChallengeShare";
import { useChallengeScore } from "../../challenge/challengeStorage";
import { FINISHED } from "./Model";

export default function GameSummary({
  gameId,
  totalRoundCount,
  onOpenRound,
  daily,
}: {
  gameId: string;
  totalRoundCount: number;
  onOpenRound: (round: number) => void;
  /** Set for a daily challenge: the buttons share the result instead of starting the same game again. */
  daily?: DailyInfo;
}): React.JSX.Element {
  const results = useQueries({
    queries: Array.from({ length: totalRoundCount }, (_v, i) => roundQuery(gameId, i)),
  });
  const { createGame, isLoading: creating, error: createError } = useCreateGame();

  const all = results.flatMap((r) => (r.data ? [r.data] : []));
  // Called before the early return below: a hook can't be skipped.
  const share = useDailyShareText(daily, all[0]);
  const friend = useChallengeScore(gameId);

  if (all.length < totalRoundCount) {
    return <LoadingSpinner fullPage className="page-bg" />;
  }

  const summary = summariseGame(all);
  const { score, maxScore, heading, gameTitle, mixed } = summary;
  const gameType = summary.playAgainType;
  const meta = [
    daily ? `Daily ${daily.date}` : (gameTitle ?? GAME_NAMES.RANDOM),
    ...DIFFICULTY_LABEL[summary.difficulty],
  ].join(" · ");
  return (
    <PageShell wide topBarEnd={<KofiBadge />}>
      <div className="gg-summary">
        <div className="gg-summary__head">
          <p className="gg-summary__meta gg-mono gg-text-muted">{meta}</p>
          <p className="gg-summary__score">
            <span>{score}</span> <span className="gg-summary__max gg-mono">/ {maxScore}</span>
          </p>
          <h1 className="gg-summary__heading">
            {heading === "perfect" ? <mark className="gg-mark">Perfect game!</mark> : "Your result"}
          </h1>
          <ul className="gg-summary__stats gg-mono">
            <li>
              <span>Perfect rounds</span>{" "}
              <span>
                {summary.perfectRounds} of {summary.rows.length}
              </span>
            </li>
            <li>
              <span>Hints used</span> <span>{summary.hintsUsed}</span>
            </li>
          </ul>
          {friend && (
            <p className="gg-summary__friend gg-mono">
              Your friend scored {friend.score} / {friend.maxScore}
            </p>
          )}
          {daily && <DailyComparison date={daily.date} difficulty={all[0].difficulty} score={score} />}
        </div>

        <div className="gg-summary__list">
          <h2 className="gg-summary__table-heading">Round by round</h2>
          <ScoreKey />
          <div className="gg-summary__row gg-summary__row--head gg-mono" aria-hidden="true">
            <span>No.</span>
            <span>Answers</span>
            <span className="gg-summary__wrong">Wrong guesses</span>
            <span className="gg-summary__points">Points</span>
          </div>
          {summary.rows.map((row, i) => (
            <SummaryRow key={i} row={row} index={i} mixed={mixed} onOpen={() => onOpenRound(i)} />
          ))}
        </div>

        <div className="gg-summary__actions">
          {!!createError && (
            <div className="gg-summary__error gg-text-danger" role="alert">
              {creationErrorMessage(createError, "Couldn't start a new game. Try again.")}
            </div>
          )}
          {share !== null && (
            <div className="gg-summary__share">
              <ShareButton text={share} />
            </div>
          )}
          {!daily && all[0].gameResult.gameState === FINISHED && (
            <div className="gg-summary__share">
              <ChallengeShare gameId={gameId} round={all[0]} />
            </div>
          )}
          <div className="gg-summary__buttons">
            {daily ? (
              <Button
                variant="secondary"
                onClick={() => createGame(RANDOM_OPTION.type, 5, all[0].difficulty)}
                disabled={creating}
              >
                {creating ? <Spinner size="sm" /> : "Play a Mixed game"}
              </Button>
            ) : (
              <Button
                variant="primary"
                onClick={() => createGame(gameType, totalRoundCount, all[0].difficulty)}
                disabled={creating}
              >
                {creating ? <Spinner size="sm" /> : "Play another"}
              </Button>
            )}
            <Link href="/" className={buttonClass({ variant: "quiet" })}>
              Home
            </Link>
          </div>
        </div>
      </div>
    </PageShell>
  );
}
