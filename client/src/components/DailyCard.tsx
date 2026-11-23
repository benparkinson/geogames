import Link from "next/link";
import { localDateString } from "../api/daily";
import { formatCountdown, secondsToNextDaily } from "../daily/countdown";
import { entryFor, useDailyEntry } from "../daily/dailyStorage";
import { useNowSeconds } from "../daily/useNowSeconds";
import Ticket, { ticketDate } from "./Ticket";

/**
 * The home page's link to the daily challenge, as a ticket: today's date, where the player is in today's one and,
 * once it is finished, the time to the next one. The date comes from the clock, so the card rolls over at midnight.
 */
export default function DailyCard(): React.JSX.Element {
  const nowSeconds = useNowSeconds();
  const now = nowSeconds === null ? null : new Date(nowSeconds * 1000);
  const todaysDaily = entryFor(useDailyEntry(), now === null ? null : localDateString(now));
  const dailyStatus = todaysDaily?.result
    ? `Done: ${todaysDaily.result.score} / ${todaysDaily.result.maxScore}`
    : todaysDaily
      ? "Continue"
      : "Start the daily";

  return (
    // A link, outside the list of games, so the arrow keys through the games don't reach it.
    <Link href="/daily" className="gg-ticket-link">
      <Ticket
        date={now === null ? null : ticketDate(now)}
        footer={
          <>
            <span className="gg-ticket__status">{dailyStatus}</span>
            {todaysDaily?.result && now !== null && (
              <span className="gg-mono" data-countdown>
                Next in {formatCountdown(secondsToNextDaily(now))}
              </span>
            )}
          </>
        }
      >
        <h2 className="gg-ticket__heading">Today&apos;s five</h2>
        <p className="gg-ticket__text">Five different geography puzzles, a new challenge every day.</p>
      </Ticket>
    </Link>
  );
}
