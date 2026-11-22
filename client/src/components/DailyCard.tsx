import Link from "next/link";
import { localDateString } from "../api/daily";
import { formatCountdown, secondsToNextDaily } from "../daily/countdown";
import { entryFor, useDailyEntry } from "../daily/dailyStorage";
import { currentStreak, useDailyStreak } from "../daily/dailyStreak";
import { useNowSeconds } from "../daily/useNowSeconds";
import { DailyIcon, StreakIcon } from "../ui/icons";

/**
 * The home page's link to the daily challenge, with where the player is in today's one, their streak and, once
 * it is finished, the time to the next one. The date comes from the clock, so the card rolls over at midnight.
 */
export default function DailyCard(): React.JSX.Element {
  const nowSeconds = useNowSeconds();
  const now = nowSeconds === null ? null : new Date(nowSeconds * 1000);
  const today = now === null ? null : localDateString(now);
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
      <div className="daily-hero__head">
        <div className="daily-hero__title">
          <DailyIcon aria-hidden="true" /> Daily challenge
        </div>
        {streak >= 1 && (
          <div className="daily-hero__streak" data-streak={streak}>
            <StreakIcon aria-hidden="true" /> <b>{streak}</b> day streak
          </div>
        )}
      </div>
      <div className="daily-hero__foot">
        <div className="daily-hero__status">{dailyStatus}</div>
        {todaysDaily?.result && now !== null && (
          <div className="daily-hero__countdown" data-countdown>
            Next daily in {formatCountdown(secondsToNextDaily(now))}
          </div>
        )}
      </div>
    </Link>
  );
}
