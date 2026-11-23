import type { SummaryRowView } from "./summaryView";
import { RESULT_ICONS } from "../../ui/icons";

/** One round of the summary: its answers, the wrong guesses, its points. A button that opens the round. */
export default function SummaryRow({
  row,
  index,
  mixed,
  onOpen,
}: {
  row: SummaryRowView;
  index: number;
  /** A mixed game names each round's type; a game of one type names it once, at the top. */
  mixed: boolean;
  onOpen: () => void;
}): React.JSX.Element {
  const Icon = RESULT_ICONS[row.result];
  const missed = row.outcome === null || row.outcome === "missed";
  const note = row.outOfGuesses ? "Ran out of lives" : row.result === "revealed" ? "Gave up" : null;
  return (
    <button type="button" className="gg-summary__row" onClick={onOpen} data-outcome={row.outcome ?? "missed"}>
      <span className="gg-summary__no gg-mono">{index + 1}.</span>
      <span className="gg-summary__answers">
        {mixed && <span className="gg-summary__note gg-text-muted">{row.label}</span>}
        <span className="gg-summary__names">
          {row.answers.map((name, i) => (
            <span key={name}>
              {i > 0 && " · "}
              {row.missed.includes(name) ? (
                <span data-outcome="missed">
                  {name}
                  <span className="gg-visually-hidden"> (not found)</span>
                </span>
              ) : (
                name
              )}
            </span>
          ))}
        </span>
        {note && <span className="gg-summary__note">{note}</span>}
        {row.hintsUsed > 0 && (
          <span className="gg-summary__note gg-text-muted">
            {row.hintsUsed === 1 ? "1 hint" : `${row.hintsUsed} hints`}
          </span>
        )}
      </span>
      <span className="gg-summary__wrong gg-mono gg-text-muted">
        {row.wrongGuessNames.length > 0 ? row.wrongGuessNames.join(", ") : "–"}
      </span>
      <span className="gg-summary__points gg-mono" data-missed={missed || undefined}>
        <Icon aria-hidden="true" /> +{row.points}
      </span>
    </button>
  );
}
