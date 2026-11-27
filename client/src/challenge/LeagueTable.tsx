import { useId } from "react";
import type { ChallengeEntry } from "../api/types";

/**
 * A challenge's league table. `yours` is the player's own row when it is below `entries`, shown after a gap.
 * `topLabel` names the first row.
 */
export default function LeagueTable({
  entries,
  yours,
  topLabel,
}: {
  entries: ChallengeEntry[];
  yours?: ChallengeEntry | null;
  topLabel?: string;
}): React.JSX.Element {
  const headingId = useId();
  const rows = yours ? [...entries, yours] : entries;
  return (
    <section className="gg-league">
      <h2 id={headingId} className="gg-league__heading gg-mono">
        League table
      </h2>
      <ol className="gg-league__list gg-mono" aria-labelledby={headingId}>
        {rows.map((entry, i) => (
          <li
            key={entry.rank}
            className="gg-league__row"
            data-you={entry.you || undefined}
            data-gap={(!!yours && i === entries.length) || undefined}
          >
            <span className="gg-league__rank">{entry.rank}</span>{" "}
            <span className="gg-league__initials">{entry.initials ?? "???"}</span>{" "}
            {entry.you && <span className="gg-league__note">You</span>}
            {i === 0 && topLabel && <span className="gg-league__note">{topLabel}</span>}{" "}
            <span className="gg-league__score">{entry.score}</span>
          </li>
        ))}
      </ol>
    </section>
  );
}
