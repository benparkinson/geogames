import { CheckLg, EyeFill, StarFill, XLg } from "react-bootstrap-icons";
import type { SummaryRowView } from "./summaryView";

const RESULT_ICON = { firstGuess: StarFill, correct: CheckLg, hints: CheckLg, outOfGuesses: XLg, revealed: EyeFill };

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
  const Icon = RESULT_ICON[row.result];
  return (
    <button
      type="button"
      className="gg-list__item d-flex justify-content-between align-items-center gap-4 text-start"
      onClick={onOpen}
    >
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
            <span className="small gg-text-muted">Out of guesses</span>
          </>
        )}
        {row.hintsUsed > 0 && (
          <>
            <br />
            <span className="small gg-text-muted">{row.hintsUsed === 1 ? "1 hint" : `${row.hintsUsed} hints`}</span>
          </>
        )}
      </span>
      <span className="text-nowrap" data-outcome={row.outcome ?? "missed"}>
        <Icon aria-hidden="true" /> +{row.points}
      </span>
    </button>
  );
}
