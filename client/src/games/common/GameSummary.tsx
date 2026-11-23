import Link from "next/link";
import { Button, buttonClass, Spinner } from "../../ui";
import KofiBadge from "../../components/KofiBadge";
import LoadingSpinner from "../../components/LoadingSpinner";
import { useQueries } from "@tanstack/react-query";
import { roundQuery, useCreateGame } from "../../api/games";
import { creationErrorMessage } from "../../api/http";
import { RANDOM_OPTION } from "../registry";
import { summariseGame } from "./summaryView";
import DailyComparison from "../../daily/DailyComparison";
import ShareButton from "../../daily/ShareButton";
import { type DailyInfo } from "../../daily/dailyStorage";
import { useDailyShareText } from "../../daily/useDailyShareText";
import PageShell from "../../components/PageShell";
import SummaryRow from "./SummaryRow";
import { RESULT_ICONS } from "../../ui/icons";

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

  if (all.length < totalRoundCount) {
    return <LoadingSpinner fullPage className="page-bg" />;
  }

  const summary = summariseGame(all);
  const { score, maxScore, heading, summaryLine, gameTitle, mixed } = summary;
  const gameType = summary.playAgainType;
  return (
    <PageShell wide topBarEnd={<KofiBadge />}>
      <div className="gg-summary">
        <div className="gg-summary__head">
          {gameTitle && <p className="gg-summary__title gg-text-muted">{gameTitle}</p>}
          <h1 className="gg-summary__heading">
            {daily && <span className="gg-text-muted">Daily {daily.date}: </span>}
            {heading === "perfect" ? (
              <>
                <RESULT_ICONS.firstGuess aria-hidden="true" className="gg-text-correct" /> Perfect game! {score} /{" "}
                {maxScore}
              </>
            ) : heading === "fullMarks" ? (
              <>
                <RESULT_ICONS.correct aria-hidden="true" className="gg-text-correct" /> Full marks! {score} / {maxScore}
              </>
            ) : (
              `You scored ${score} / ${maxScore}`
            )}
          </h1>
          <p className="gg-summary__line gg-text-muted">{summaryLine}</p>
          {daily && <DailyComparison date={daily.date} difficulty={all[0].difficulty} score={score} />}
        </div>

        <div className="gg-list gg-summary__list">
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
                {creating ? <Spinner size="sm" /> : "Play again"}
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
