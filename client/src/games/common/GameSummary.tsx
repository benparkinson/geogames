import Link from "next/link";
import { Button, buttonClass, Spinner } from "../../ui";
import { CheckLg, EyeFill, StarFill } from "react-bootstrap-icons";
import HomeLink from "../../components/HomeLink";
import ThemeToggle from "../../components/ThemeToggle";
import KofiBadge from "../../components/KofiBadge";
import LoadingSpinner from "../../components/LoadingSpinner";
import { useQueries } from "@tanstack/react-query";
import { roundQuery, useCreateGame } from "../../api/games";
import { GameRoundView } from "../../api/types";
import { findGameModule, RANDOM_OPTION } from "../registry";
import { CORRECT_ANSWER, isRevealed, OUT_OF_GUESSES } from "./Model";
import { isPerfect } from "./scoring";

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

export default function GameSummary({ gameId, totalRoundCount, onOpenRound }: {
  gameId: string;
  totalRoundCount: number;
  onOpenRound: (round: number) => void;
}): React.JSX.Element {
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

  return (
    <div className="min-vh-100 page-bg">
      <div className="px-3 py-2 d-flex justify-content-between align-items-center">
        <div className="d-flex align-items-center gap-2">
          <HomeLink />
          <ThemeToggle />
        </div>
        <KofiBadge />
      </div>
      <div className="card mx-auto my-4 p-3" style={{ maxWidth: 560 }}>
        <h1 className="h3 mb-1">
          {perfectGame
            ? <><StarFill aria-hidden="true" className="text-warning" /> Perfect game! {score} / {maxScore}</>
            : `You scored ${score} / ${maxScore}`}
        </h1>
        <p className="text-muted mb-3">{breakdown.length > 0 ? breakdown.join(" · ") : "0 perfect"}</p>

        <div className="list-group mb-3">
          {all.map((r, i) => {
            const isCorrect = r.answerState === CORRECT_ANSWER;
            const perfect = isPerfect(r.answerState, r.hintsUsed);
            return (
              <button
                key={i}
                type="button"
                className="list-group-item list-group-item-action d-flex justify-content-between align-items-center gap-2 text-start"
                onClick={() => onOpenRound(i)}
              >
                <span>
                  <span className="text-muted">{i + 1}. {roundLabel(r)}</span>
                  <br />
                  {roundAnswers(r).join(", ")}
                  {r.answerState === OUT_OF_GUESSES && (
                    <>
                      <br />
                      <span className="small text-muted">Out of guesses</span>
                    </>
                  )}
                  {r.hintsUsed > 0 && (
                    <>
                      <br />
                      <span className="small text-muted">{r.hintsUsed === 1 ? "1 hint" : `${r.hintsUsed} hints`}</span>
                    </>
                  )}
                </span>
                <span className={"text-nowrap " + (isCorrect ? "text-success" : "text-secondary")}>
                  {perfect ? <StarFill aria-hidden="true" />
                    : isCorrect ? <CheckLg aria-hidden="true" />
                    : <EyeFill aria-hidden="true" />} +{r.points}
                </span>
              </button>
            );
          })}
        </div>

        {!!createError && <div className="text-danger small mb-2" role="alert">Couldn&apos;t start a new game. Try again.</div>}
        <div className="d-flex gap-2">
          <Button variant="primary" onClick={() => createGame(gameType, totalRoundCount)} disabled={creating}>
            {creating ? <Spinner size="sm" /> : "Play again"}
          </Button>
          <Link href="/" className={buttonClass({ variant: "quiet" })}>Home</Link>
        </div>
      </div>
    </div>
  );
}
