import Link from "next/link";
import { Button, buttonClass, Spinner } from "../../ui";
import { CheckLg, EyeFill, StarFill } from "react-bootstrap-icons";
import HomeLink from "../../components/HomeLink";
import ThemeToggle from "../../components/ThemeToggle";
import KofiBadge from "../../components/KofiBadge";
import SiteFooter from "../../components/SiteFooter";
import LoadingSpinner from "../../components/LoadingSpinner";
import { useQueries } from "@tanstack/react-query";
import { roundQuery, useCreateGame } from "../../api/games";
import { GameRoundView } from "../../api/types";
import { findGameModule, RANDOM_OPTION } from "../registry";
import { CORRECT_ANSWER, FINISHED, isRevealed, OUT_OF_GUESSES } from "./Model";
import { isPerfect } from "./scoring";
import ShareButton from "../../daily/ShareButton";
import { useDailyEntry } from "../../daily/dailyStorage";
import { resultFromGame } from "../../daily/result";
import { shareText } from "../../daily/shareText";

function roundLabel(round: GameRoundView): string {
  return findGameModule(round.gameType.name)?.singular ?? round.gameType.name;
}

function roundAnswers(round: GameRoundView): string[] {
  return findGameModule(round.gameType.name)?.answerNames(round.data) ?? [];
}

/** The game type to start for "Play again": the one every round used, or a mix if they differed. */
function playAgainType(rounds: GameRoundView[]): string {
  const types = new Set(rounds.map(r => r.gameType.name));
  return types.size === 1 ? rounds[0].gameType.name : RANDOM_OPTION.type;
}

export default function GameSummary({ gameId, totalRoundCount, onOpenRound, daily }: {
  gameId: string;
  totalRoundCount: number;
  onOpenRound: (round: number) => void;
  /** Set for a daily challenge: the buttons share the result instead of starting the same game again. */
  daily?: { number: number; date: string };
}): React.JSX.Element {
  const dailyEntry = useDailyEntry();
  const results = useQueries({
    queries: Array.from({ length: totalRoundCount }, (_v, i) => roundQuery(gameId, i)),
  });
  const { createGame, isLoading: creating, error: createError } = useCreateGame();

  const all = results.flatMap(r => (r.data ? [r.data] : []));

  if (all.length < totalRoundCount) {
    return <LoadingSpinner fullPage className="page-bg" />;
  }

  // Every round carries the whole game's result.
  const { score, maxScore, perfectRounds, roundsCorrect, roundAnswerStates } = all[0].gameResult;
  const withHints = roundsCorrect - perfectRounds;
  const revealed = roundAnswerStates.filter(isRevealed).length;
  const perfectGame = perfectRounds === totalRoundCount;
  const breakdown = [
    perfectRounds > 0 && `${perfectRounds} perfect`,
    withHints > 0 && `${withHints} with hints`,
    revealed > 0 && `${revealed} revealed`,
  ].filter(Boolean);
  const gameType = playAgainType(all);
  const share = daily && all[0].gameResult.gameState === FINISHED
    ? shareText(
        { number: daily.number, hardcore: dailyEntry?.date === daily.date && dailyEntry.hardcore, result: resultFromGame(all[0].gameResult) },
        window.location.origin,
      )
    : null;

  return (
    <div className="min-vh-100 page-bg d-flex flex-column">
      <div className="px-3 py-2 d-flex justify-content-between align-items-center">
        <div className="d-flex align-items-center gap-2">
          <HomeLink />
          <ThemeToggle />
        </div>
        <KofiBadge />
      </div>
      <div className="gg-card mx-auto my-4 p-3">
        <h1 className="h3 mb-1">
          {daily && <span className="gg-text-muted">Daily #{daily.number}: </span>}
          {perfectGame
            ? <><StarFill aria-hidden="true" className="gg-text-perfect" /> Perfect game! {score} / {maxScore}</>
            : `You scored ${score} / ${maxScore}`}
        </h1>
        <p className="gg-text-muted mb-3">{breakdown.length > 0 ? breakdown.join(" · ") : "0 perfect"}</p>

        <div className="gg-list mb-3">
          {all.map((r, i) => {
            const isCorrect = r.answerState === CORRECT_ANSWER;
            const perfect = isPerfect(r.answerState, r.hintsUsed);
            return (
              <button
                key={i}
                type="button"
                className="gg-list__item d-flex justify-content-between align-items-center gap-2 text-start"
                onClick={() => onOpenRound(i)}
              >
                <span>
                  <span className="gg-text-muted">{i + 1}. {roundLabel(r)}</span>
                  <br />
                  {roundAnswers(r).join(", ")}
                  {r.answerState === OUT_OF_GUESSES && (
                    <>
                      <br />
                      <span className="small gg-text-muted">Out of guesses</span>
                    </>
                  )}
                  {r.hintsUsed > 0 && (
                    <>
                      <br />
                      <span className="small gg-text-muted">{r.hintsUsed === 1 ? "1 hint" : `${r.hintsUsed} hints`}</span>
                    </>
                  )}
                </span>
                <span className={"text-nowrap " + (isCorrect ? "gg-text-correct" : "gg-text-revealed")}>
                  {perfect ? <StarFill aria-hidden="true" />
                    : isCorrect ? <CheckLg aria-hidden="true" />
                    : <EyeFill aria-hidden="true" />} +{r.points}
                </span>
              </button>
            );
          })}
        </div>

        {!!createError && <div className="gg-text-danger small mb-2" role="alert">Couldn&apos;t start a new game. Try again.</div>}
        {share !== null && <div className="mb-3"><ShareButton text={share} /></div>}
        <div className="d-flex gap-2">
          {daily ? (
            <Button variant="secondary" onClick={() => createGame("RANDOM", 5)} disabled={creating}>
              {creating ? <Spinner size="sm" /> : "Play a Mixed game"}
            </Button>
          ) : (
            <Button variant="primary" onClick={() => createGame(gameType, totalRoundCount)} disabled={creating}>
              {creating ? <Spinner size="sm" /> : "Play again"}
            </Button>
          )}
          <Link href="/" className={buttonClass({ variant: "quiet" })}>Home</Link>
        </div>
      </div>
      <SiteFooter className="mt-auto pt-4 pb-4" />
    </div>
  );
}
