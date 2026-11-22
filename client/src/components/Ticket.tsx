import { DailyIcon } from "../ui/icons";

const DAYS = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
const MONTHS = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];

/** A date as `Sun 22 Nov 2026`. */
export function ticketDate(date: Date): string {
  return `${DAYS[date.getDay()]} ${date.getDate()} ${MONTHS[date.getMonth()]} ${date.getFullYear()}`;
}

/**
 * The daily challenge as a ticket: a highlighter strip with "Daily challenge" and the date, a white body and an
 * ink footer bar. `date` is null while pre-rendering, as the date comes from the clock. `stripTag` is "h1" on
 * the daily pages, whose title it is.
 */
export default function Ticket({
  date,
  stripTag: Strip = "div",
  children,
  footer,
}: {
  date: string | null;
  stripTag?: "div" | "h1";
  children: React.ReactNode;
  footer: React.ReactNode;
}): React.JSX.Element {
  return (
    <div className="gg-ticket">
      <div className="gg-ticket__strip">
        <Strip className="gg-ticket__label">
          <DailyIcon aria-hidden="true" /> Daily challenge
        </Strip>
        {date !== null && <span className="gg-mono gg-ticket__date">{date}</span>}
      </div>
      <div className="gg-ticket__body">{children}</div>
      <div className="gg-ticket__foot">{footer}</div>
    </div>
  );
}
