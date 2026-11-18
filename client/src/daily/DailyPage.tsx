import { useEffect, useState } from "react";
import { wakeServer } from "../api/health";
import { hasStatus } from "../api/http";
import { localDateString, useCreateDailyGame } from "../api/daily";
import { useRound } from "../api/games";
import { FINISHED } from "../games/common/Model";
import GameRoundPage from "../games/common/GameRoundPage";
import HomeLink from "../components/HomeLink";
import ThemeToggle from "../components/ThemeToggle";
import SiteFooter from "../components/SiteFooter";
import LoadingSpinner from "../components/LoadingSpinner";
import HardModeToggle from "../components/HardModeToggle";
import { useHardMode } from "../settings/hardMode";
import { Button, Notice, Spinner } from "../ui";
import ShareButton from "./ShareButton";
import { clearDaily, DailyEntry, entryFor, readDaily, useDailyEntry, writeDaily } from "./dailyStorage";
import { resultFromGame } from "./result";
import { shareText } from "./shareText";
import { useToday } from "./useToday";

function Shell({ children }: { children: React.ReactNode }): React.JSX.Element {
  return (
    <div className="min-vh-100 page-bg d-flex flex-column">
      <div className="px-3 py-2 d-flex align-items-center gap-2">
        <HomeLink />
        <ThemeToggle />
      </div>
      <div className="gg-card mx-auto my-4 p-3">{children}</div>
      <SiteFooter className="mt-auto pt-4 pb-4" />
    </div>
  );
}

function Intro(): React.JSX.Element {
  const { mutate, isPending, error } = useCreateDailyGame();
  const [hardMode] = useHardMode();
  const [slow, setSlow] = useState(false);

  // Start waking a cold server while the player reads.
  useEffect(() => {
    wakeServer();
  }, []);

  useEffect(() => {
    if (!isPending) return;
    const timer = setTimeout(() => setSlow(true), 3000);
    return () => {
      clearTimeout(timer);
      setSlow(false);
    };
  }, [isPending]);

  // On click and not on load: link previews and crawlers fetch this page, and must not make games.
  function play(): void {
    // Read the date now, so a page left open past midnight starts the new day's game.
    const date = localDateString();
    // Another tab may have started the game since this page loaded.
    if (entryFor(readDaily(), date)) return;
    mutate(date, {
      onSuccess: (game) => {
        writeDaily({ v: 1, date, number: game.number, gameId: game.id, hardcore: hardMode });
      },
    });
  }

  return (
    <Shell>
      <h1 className="h3 mb-1">Daily challenge</h1>
      <p className="mb-3">Five rounds, one of each kind, the same for everyone today.</p>
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
    </Shell>
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
    <Shell>
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
    </Shell>
  );
}

/**
 * Shows the player's game and keeps their entry up to date: the result once the game is finished, Hard mode
 * turned off for good if it ever is, and no entry if the server has deleted the game.
 */
function Game({ entry, onFinished }: { entry: DailyEntry; onFinished: () => void }): React.JSX.Element {
  const first = useRound(entry.gameId, 0);
  const [hardMode] = useHardMode();
  const finished = first.data?.gameResult.gameState === FINISHED;
  const gone = hasStatus(first.error, 404);
  const hasResult = entry.result !== undefined;
  const result = first.data?.gameResult;

  useEffect(() => {
    if (finished && !hasResult && result) {
      writeDaily({ ...entry, result: resultFromGame(result) });
      onFinished();
    }
  }, [finished, hasResult, result, entry, onFinished]);

  useEffect(() => {
    // Only a game played in Hard mode throughout earns the marker.
    if (!hardMode && entry.hardcore && !hasResult) {
      writeDaily({ ...entry, hardcore: false });
    }
  }, [hardMode, entry, hasResult]);

  useEffect(() => {
    // The game is deleted about a day after it is made. A finished entry keeps its result for sharing.
    if (gone && !hasResult) clearDaily();
  }, [gone, hasResult]);

  return <GameRoundPage gameId={entry.gameId} daily={{ number: entry.number, date: entry.date }} />;
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
