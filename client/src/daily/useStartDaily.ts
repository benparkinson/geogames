import { localDateString, useCreateDailyGame } from "../api/daily";
import { useDifficulty } from "../settings/difficulty";
import { entryFor, readDaily, writeDaily } from "./dailyStorage";

/** Starts today's daily game and saves it. Call `play` on a click, never on load: crawlers fetch the page. */
export function useStartDaily(): { play: () => void; isPending: boolean; error: unknown } {
  const { mutate, isPending, error } = useCreateDailyGame();
  const [difficulty] = useDifficulty();

  function play(): void {
    // Read the date now, so a page left open past midnight starts the new day's game.
    const date = localDateString();
    // Another tab may have started the game since this page loaded.
    if (entryFor(readDaily(), date)) return;
    mutate(
      { date, difficulty },
      {
        onSuccess: (game) => {
          writeDaily({ v: 1, date, number: game.number, gameId: game.id, difficulty });
        },
      },
    );
  }

  return { play, isPending, error };
}
