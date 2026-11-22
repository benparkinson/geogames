import type { SummaryRowView } from "./summaryView";
import { RESULT_ICONS } from "../../ui/icons";

/** One round of the summary: its answers, how it went and its points. A button that opens the round. */
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
  return (
    <button type="button" className="gg-list__item gg-summary__row" onClick={onOpen}>
      <span>
        {mixed ? (
          <>
            <span className="gg-text-muted">
              {index + 1}. {row.label}
            </span>
            <br />
            {row.answers.join(", ")}
          </>
        ) : (
          <>
            <span className="gg-text-muted">{index + 1}.</span> {row.answers.join(", ")}
          </>
        )}
        {row.outOfGuesses && (
          <>
            <br />
            <span className="gg-summary__note gg-text-muted">Out of guesses</span>
          </>
        )}
        {row.hintsUsed > 0 && (
          <>
            <br />
            <span className="gg-summary__note gg-text-muted">
              {row.hintsUsed === 1 ? "1 hint" : `${row.hintsUsed} hints`}
            </span>
          </>
        )}
      </span>
      <span className="gg-summary__points" data-outcome={row.outcome ?? "missed"}>
        <Icon aria-hidden="true" /> +{row.points}
      </span>
    </button>
  );
}
