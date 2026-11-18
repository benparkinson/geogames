import Link from "next/link";
import { CalendarCheck } from "react-bootstrap-icons";
import { entryFor, useDailyEntry } from "../daily/dailyStorage";
import { useToday } from "../daily/useToday";

/** The home page's link to the daily challenge, with where the player is in today's one. */
export default function DailyCard(): React.JSX.Element {
  const todaysDaily = entryFor(useDailyEntry(), useToday());
  const dailyStatus = todaysDaily?.result
    ? `Done: ${todaysDaily.result.score} / ${todaysDaily.result.maxScore}`
    : todaysDaily
      ? "Continue"
      : "Five different geography puzzles, the same for everyone today.";

  return (
    // A link, outside the radio group of games, so the arrow keys through the games don't reach it.
    <Link href="/daily" className="game-card d-block w-100 text-start text-decoration-none p-3 mb-4">
      <div className="fw-bold">
        <CalendarCheck aria-hidden="true" /> Daily challenge
      </div>
      <div className="small gg-text-muted">{dailyStatus}</div>
    </Link>
  );
}
