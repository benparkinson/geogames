import { useEffect, useState } from "react";
import { useWakeServer } from "../api/health";
import { hasStatus } from "../api/http";
import { localDateString, useCreateDailyGame } from "../api/daily";
import { useRound } from "../api/games";
import { FINISHED } from "../games/common/Model";
import GameRoundPage from "../games/common/GameRoundPage";
import PageShell from "../components/PageShell";
import LoadingSpinner from "../components/LoadingSpinner";
import HardModeToggle from "../components/HardModeToggle";
import { useHardMode } from "../settings/hardMode";
import { Button, Notice, Spinner } from "../ui";
import { useDelayedFlag } from "../ui/useDelayedFlag";
import ShareButton from "./ShareButton";
import { clearDaily, DailyEntry, entryFor, readDaily, useDailyEntry, writeDaily } from "./dailyStorage";
import { resultFromGame } from "./result";
import { shareText } from "./shareText";
import { useToday } from "./useToday";

function Intro(): React.JSX.Element {
  const { mutate, isPending, error } = useCreateDailyGame();
  const [hardMode] = useHardMode();
  const slow = useDelayedFlag(isPending, 3000);
  // Start waking a cold server while the player reads.
  useWakeServer();

  // On click and not on load: link previews and crawlers fetch this page, and must not make games.
  function play(): void {
    // Read the date now, so a page left open past midnight starts the new day's game.
    const date = localDateString();
    // Another tab may have started the game since this page loaded.
    if (entryFor(readDaily(), date)) return;
    mutate(
      { date, hardMode },
      {
        onSuccess: (game) => {
          writeDaily({ v: 1, date, number: game.number, gameId: game.id, hardcore: hardMode });
        },
      },
    );
  }

  return (
    <PageShell>
      <h1 className="h3 mb-1">Daily challenge</h1>
      <p className="mb-3">
        Five different geography puzzles, the same for everyone today. Each round tells you what to name and what the
        map shows. Hints help, but they lower your score.
      </p>
      <div className="mb-3">
        <HardModeToggle />
      </div>
      <Button variant="primary" onClick={play} disabled={isPending}>
        {isPending ? (
          <>
            <Spinner size="sm" decorative /> Starting…
          </>
        ) : (
          "Play today's challenge"
        )}
      </Button>
      <div aria-live="polite" className="mt-2">
        {isPending && slow && <p className="gg-text-muted mb-0">Waking up the server…</p>}
        {!!error && !isPending && (
          <Notice className="mb-0">Couldn&apos;t start. The server may be down. Try again.</Notice>
        )}
      </div>
    </PageShell>
  );
}

function Finished({
  entry,
  onOpenGame,
}: {
  entry: DailyEntry & { result: NonNullable<DailyEntry["result"]> };
  onOpenGame: () => void;
}): React.JSX.Element {
  const text = shareText({ date: entry.date, hardMode: entry.hardcore, result: entry.result }, window.location.origin);
  return (
    <PageShell>
      <h1 className="h3 mb-1">Daily {entry.date}</h1>
      <p className="mb-3">
        You scored {entry.result.score} / {entry.result.maxScore}
      </p>
      <pre className="mb-3" aria-label="Your result">
        {text}
      </pre>
      <div className="d-flex flex-wrap gap-2">
        <ShareButton text={text} />
        <Button variant="secondary" onClick={onOpenGame}>
          See the rounds
        </Button>
      </div>
    </PageShell>
  );
}

/**
 * Shows the player's game and keeps their entry up to date: the result and the game's Hard mode setting once it is
 * finished, and no entry if the server has deleted the game.
 */
function Game({ entry, onFinished }: { entry: DailyEntry; onFinished: () => void }): React.JSX.Element {
  const first = useRound(entry.gameId, 0);
  const game = first.data;
  const gone = hasStatus(first.error, 404);
  const hasResult = entry.result !== undefined;

  useEffect(() => {
    // One write: the result and the setting together, so two writes from one stale entry can't overwrite each other.
    if (game?.gameResult.gameState === FINISHED && !hasResult) {
      writeDaily({ ...entry, hardcore: game.hardMode, result: resultFromGame(game.gameResult) });
      onFinished();
    }
  }, [game, hasResult, entry, onFinished]);

  useEffect(() => {
    // The game is deleted about a day after it is made. A finished entry keeps its result for sharing.
    if (gone && !hasResult) clearDaily();
  }, [gone, hasResult]);

  return <GameRoundPage gameId={entry.gameId} daily={{ date: entry.date }} />;
}

export default function DailyPage(): React.JSX.Element {
  const today = useToday();
  const stored = useDailyEntry();
  const [openGame, setOpenGame] = useState(false);
  const entry = entryFor(stored, today);

  if (today === null) {
    return <LoadingSpinner fullPage className="gg-text-correct" />;
  }
  if (entry === null) {
    return <Intro />;
  }
  if (entry.result && !openGame) {
    return <Finished entry={{ ...entry, result: entry.result }} onOpenGame={() => setOpenGame(true)} />;
  }
  return <Game entry={entry} onFinished={() => setOpenGame(true)} />;
}
