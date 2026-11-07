import Link from "next/link";
import { Button, Spinner } from "react-bootstrap";
import { CheckLg, EyeFill } from "react-bootstrap-icons";
import HomeLink from "../../components/HomeLink";
import LoadingSpinner from "../../components/LoadingSpinner";
import { useQueries } from "@tanstack/react-query";
import { roundQuery, useCreateGame } from "../../api/games";
import { GameRoundView } from "../../api/types";
import { findGameModule, RANDOM_OPTION } from "../registry";
import { CORRECT_ANSWER, UNANSWERED } from "./Model";

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

  const correct = all.filter(r => r.answerState === CORRECT_ANSWER).length;
  const revealed = all.filter(r => r.answerState !== CORRECT_ANSWER && r.answerState !== UNANSWERED).length;
  const gameType = playAgainType(all);

  return (
    <div className="min-vh-100 page-bg">
      <div className="px-3 py-2">
        <HomeLink />
      </div>
      <div className="card mx-auto my-4 p-3" style={{ maxWidth: 560 }}>
        <h1 className="h3 mb-1">You scored {correct} / {totalRoundCount}</h1>
        <p className="text-muted mb-3">{correct} correct · {revealed} revealed</p>

        <div className="list-group mb-3">
          {all.map((r, i) => {
            const isCorrect = r.answerState === CORRECT_ANSWER;
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
                </span>
                <span className={"text-nowrap " + (isCorrect ? "text-success" : "text-secondary")}>
                  {isCorrect
                    ? <><CheckLg aria-hidden="true" /> Correct</>
                    : <><EyeFill aria-hidden="true" /> Revealed</>}
                </span>
              </button>
            );
          })}
        </div>

        {!!createError && <div className="text-danger small mb-2" role="alert">Couldn&apos;t start a new game. Try again.</div>}
        <div className="d-flex gap-2">
          <Button variant="primary" onClick={() => createGame(gameType, totalRoundCount)} disabled={creating}>
            {creating ? <Spinner animation="border" size="sm" /> : "Play again"}
          </Button>
          <Link href="/" className="btn btn-outline-secondary">Home</Link>
        </div>
      </div>
    </div>
  );
}
