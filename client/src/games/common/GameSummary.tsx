import Link from "next/link";
import { Button, buttonClass, Spinner } from "../../ui";
import { StarFill } from "react-bootstrap-icons";
import KofiBadge from "../../components/KofiBadge";
import LoadingSpinner from "../../components/LoadingSpinner";
import { useQueries } from "@tanstack/react-query";
import { roundQuery, useCreateGame } from "../../api/games";
import { Difficulty, GameRoundView } from "../../api/types";
import { findGameModule, RANDOM_OPTION } from "../registry";
import { isPartlyCorrect, isRevealed } from "./Model";
import DailyComparison from "../../daily/DailyComparison";
import ShareButton from "../../daily/ShareButton";
import { type DailyInfo } from "../../daily/dailyStorage";
import { useDailyShareText } from "../../daily/useDailyShareText";
import PageShell from "../../components/PageShell";
import SummaryRow from "./SummaryRow";

/** The game type to start for "Play again": the one every round used, or a mix if they differed. */
/** What the end of the breakdown line says about the difficulty: nothing for Normal. */
const DIFFICULTY_LABEL: Record<Difficulty, string[]> = { EASY: ["Easy"], NORMAL: [], HARD: ["Hard mode"] };

function playAgainType(rounds: GameRoundView[]): string {
  const types = new Set(rounds.map((r) => r.gameType.name));
  return types.size === 1 ? rounds[0].gameType.name : RANDOM_OPTION.type;
}

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

  // Every round carries the whole game's result.
  const { score, maxScore, perfectRounds, roundsCorrect, roundAnswerStates, roundPoints } = all[0].gameResult;
  const withHints = roundsCorrect - perfectRounds;
  const partly = roundAnswerStates.filter((state, i) => isPartlyCorrect(state, roundPoints[i] ?? 0)).length;
  const revealed = roundAnswerStates.filter(isRevealed).length - partly;
  const perfectGame = perfectRounds === totalRoundCount;
  const breakdown = [
    perfectRounds > 0 && `${perfectRounds} perfect`,
    withHints > 0 && `${withHints} with hints`,
    partly > 0 && `${partly} partly correct`,
    revealed > 0 && `${revealed} revealed`,
  ].filter(Boolean);
  const outcomes = breakdown.length > 0 ? breakdown : ["0 perfect"];
  const summaryLine = [...outcomes, ...DIFFICULTY_LABEL[all[0].difficulty]].join(" · ");
  const gameType = playAgainType(all);
  // A game of one type names it once at the top; a mixed game (the daily or Mixed) names each round's type.
  const mixed = gameType === RANDOM_OPTION.type;
  const gameTitle = mixed ? null : (findGameModule(gameType)?.title ?? gameType);
  return (
    <PageShell wide compactFooter topBarEnd={<KofiBadge />}>
      {gameTitle && <p className="gg-text-muted mb-1">{gameTitle}</p>}
      <h1 className="h3 mb-1">
        {daily && <span className="gg-text-muted">Daily {daily.date}: </span>}
        {perfectGame ? (
          <>
            <StarFill aria-hidden="true" className="gg-text-perfect" /> Perfect game! {score} / {maxScore}
          </>
        ) : (
          `You scored ${score} / ${maxScore}`
        )}
      </h1>
      <p className="gg-text-muted mb-3">{summaryLine}</p>
      {daily && <DailyComparison date={daily.date} difficulty={all[0].difficulty} score={score} />}

      <div className="gg-list mb-3">
        {all.map((r, i) => (
          <SummaryRow key={i} round={r} index={i} mixed={mixed} onOpen={() => onOpenRound(i)} />
        ))}
      </div>

      {!!createError && (
        <div className="gg-text-danger small mb-2" role="alert">
          Couldn&apos;t start a new game. Try again.
        </div>
      )}
      {share !== null && (
        <div className="mb-3">
          <ShareButton text={share} />
        </div>
      )}
      <div className="d-flex gap-2">
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
    </PageShell>
  );
}
