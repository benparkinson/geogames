import Link from "next/link";
import { entryFor, useDailyEntry } from "../daily/dailyStorage";
import { currentStreak, useDailyStreak } from "../daily/dailyStreak";
import { useToday } from "../daily/useToday";
import { DailyIcon, StreakIcon } from "../ui/icons";

/** The home page's link to the daily challenge, with where the player is in today's one and their streak. */
export default function DailyCard(): React.JSX.Element {
  const today = useToday();
  const todaysDaily = entryFor(useDailyEntry(), today);
  const streak = currentStreak(useDailyStreak(), today);
  const dailyStatus = todaysDaily?.result
    ? `Done: ${todaysDaily.result.score} / ${todaysDaily.result.maxScore}`
    : todaysDaily
      ? "Continue"
      : "Five different geography puzzles, a new challenge every day.";

  return (
    // A link, outside the radio group of games, so the arrow keys through the games don't reach it.
    <Link href="/daily" className="daily-hero">
      <div className="daily-hero__main">
        <div className="daily-hero__title">
          <DailyIcon aria-hidden="true" /> Daily challenge
        </div>
        <div className="daily-hero__status">{dailyStatus}</div>
      </div>
      {streak >= 1 && (
        <div className="daily-hero__streak" data-streak={streak}>
          <div className="daily-hero__streak-count">
            <StreakIcon aria-hidden="true" /> {streak}
          </div>
          <div className="daily-hero__streak-caption">{streak === 1 ? "day streak" : "days in a row"}</div>
        </div>
      )}
    </Link>
  );
}
