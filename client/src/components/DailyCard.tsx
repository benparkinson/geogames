import Link from "next/link";
import { entryFor, useDailyEntry } from "../daily/dailyStorage";
import { useToday } from "../daily/useToday";
import { DailyIcon } from "../ui/icons";

/** The home page's link to the daily challenge, with where the player is in today's one. */
export default function DailyCard(): React.JSX.Element {
  const todaysDaily = entryFor(useDailyEntry(), useToday());
  const dailyStatus = todaysDaily?.result
    ? `Done: ${todaysDaily.result.score} / ${todaysDaily.result.maxScore}`
    : todaysDaily
      ? "Continue"
      : "Five different geography puzzles, a new challenge every day.";

  return (
    // A link, outside the radio group of games, so the arrow keys through the games don't reach it.
    <Link href="/daily" className="daily-hero d-block w-100 text-start text-decoration-none p-4 mb-4">
      <div className="h4 fw-bold mb-1">
        <DailyIcon aria-hidden="true" /> Daily challenge
      </div>
      <div className="daily-hero__status">{dailyStatus}</div>
    </Link>
  );
}
