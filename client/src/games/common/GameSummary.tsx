import Link from "next/link";
import { Button, Spinner } from "react-bootstrap";
import { CheckLg, EyeFill, HouseFill } from "react-bootstrap-icons";
import { useQueries } from "react-query";
import { fetchRound, roundUrl, useCreateGame } from "../../api/games";
import { answerNames, gameTypeLabel } from "./answers";
import { CORRECT_ANSWER, GameRoundModel } from "./Model";

export default function GameSummary({ gameId, totalRoundCount, onOpenRound }: {
  gameId: string;
  totalRoundCount: number;
  onOpenRound: (round: number) => void;
}): JSX.Element {
  const results = useQueries(
    Array.from({ length: totalRoundCount }, (_v, i) => ({
      queryKey: roundUrl(gameId, i),
      queryFn: () => fetchRound(gameId, i),
    }))
  );
  const { createGame, isLoading: creating, error: createError } = useCreateGame();

  const rounds = results.map(r => r.data as GameRoundModel | undefined);
  const loaded = rounds.every(r => !!r);

  if (!loaded) {
    return (
      <div className="d-flex min-vh-100 justify-content-center align-items-center" style={{ background: "var(--gg-bg)" }}>
        <Spinner animation="border" />
      </div>
    );
  }

  const all = rounds as GameRoundModel[];
  const correct = all.filter(r => r.answerState === CORRECT_ANSWER).length;
  const revealed = all.filter(r => r.answerState !== CORRECT_ANSWER && r.answerState !== "UNANSWERED").length;
  const types = new Set(all.map(r => r.gameType.name));
  const gameType = types.size === 1 ? all[0].gameType.name : "RANDOM";

  return (
    <div className="min-vh-100" style={{ background: "var(--gg-bg)" }}>
      <div className="px-3 py-2">
        <Link href="/" aria-label="Home" className="text-decoration-none text-body fw-semibold">
          <HouseFill aria-hidden="true" /> Geogames
        </Link>
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
                  <span className="text-muted">{i + 1}. {gameTypeLabel(r.gameType.name)}</span>
                  <br />
                  {answerNames(r).join(", ")}
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
