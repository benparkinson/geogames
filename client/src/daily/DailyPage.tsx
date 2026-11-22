import { useEffect, useState } from "react";
import { hasStatus } from "../api/http";
import { useRound } from "../api/games";
import { FINISHED } from "../games/common/Model";
import GameRoundPage from "../games/common/GameRoundPage";
import LoadingSpinner from "../components/LoadingSpinner";
import DailyIntro from "./DailyIntro";
import DailyFinished from "./DailyFinished";
import { clearDaily, DailyEntry, entryFor, useDailyEntry, writeDaily } from "./dailyStorage";
import { resultFromGame } from "./result";
import { useToday } from "./useToday";

/**
 * Shows the player's game and keeps their entry up to date: the result and the game's difficulty once it is
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
      writeDaily({ ...entry, difficulty: game.difficulty, result: resultFromGame(game.gameResult) });
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
    return <DailyIntro />;
  }
  if (entry.result && !openGame) {
    return <DailyFinished entry={{ ...entry, result: entry.result }} onOpenGame={() => setOpenGame(true)} />;
  }
  return <Game entry={entry} onFinished={() => setOpenGame(true)} />;
}
