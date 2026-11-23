import { compassPoint, formatKm } from "./distance";
import { GuessClue, GuessState, foundAnswers } from "./guessReducer";
import { GuessRow, guessListRows } from "./guessRows";
import { BearingIcon, FoundIcon, WrongGuessIcon } from "../../ui/icons";

/** The round's guesses as a numbered, ruled list: found answers, wrong guesses with their clue, and what is left. */
export default function GuessList({
  state,
  answerCount,
  allAnswers,
  showClues,
}: {
  state: GuessState;
  answerCount: number;
  allAnswers: string[];
  /** False in Hard mode, which hides the distance clues. */
  showClues: boolean;
}): React.JSX.Element {
  const rows = guessListRows(state, answerCount, allAnswers);
  // With one answer there is nothing to count until a guess is made.
  if (rows.length === 0) {
    return <></>;
  }
  const found = foundAnswers(state).length;

  return (
    <section className="guess-list" role="group" aria-label={`Answers: ${found} of ${answerCount} found`}>
      {/* One answer needs no count: the question already says what is missing. */}
      {answerCount > 1 && (
        <div className="guess-list__head">
          <span className="guess-list__count">{`${found}/${answerCount} found`}</span>
        </div>
      )}
      <ol className="guess-list__rows">
        {rows.map((row, i) => (
          <li
            key={i}
            className="guess-list__row"
            data-kind={row.kind}
            data-outcome={row.kind === "missed" ? "missed" : undefined}
            data-testid={row.kind === "wrong" ? "wrong-guess" : undefined}
          >
            <span className="guess-list__number">{i + 1}</span>
            <RowBody row={row} showClues={showClues} />
          </li>
        ))}
      </ol>
    </section>
  );
}

function RowBody({ row, showClues }: { row: GuessRow; showClues: boolean }): React.JSX.Element {
  switch (row.kind) {
    case "found":
      return (
        <>
          <span className="guess-list__name">{row.text}</span>
          <span className="guess-list__note gg-mono">
            <FoundIcon aria-hidden="true" /> found
          </span>
        </>
      );
    case "wrong":
      return (
        <>
          <span className="guess-list__name">{row.text}</span>
          <span className="guess-list__note gg-mono">
            {showClues && row.clue && <ClueBadge clue={row.clue} />} <WrongGuessIcon aria-hidden="true" />
          </span>
        </>
      );
    case "missed":
      return (
        <>
          <span className="guess-list__name">{row.text}</span>
          <span className="guess-list__note gg-mono">
            not found<span className="gg-visually-hidden"> (revealed)</span>
          </span>
        </>
      );
    case "empty":
      return (
        <span className="guess-list__name guess-list__name--empty" aria-label="Not found yet">
          —
        </span>
      );
  }
}

/** The distance and an arrow towards the answer. */
function ClueBadge({ clue }: { clue: GuessClue }): React.JSX.Element {
  return (
    <span className="guess-list__clue">
      {formatKm(clue.km)} km <BearingIcon aria-hidden="true" style={{ transform: `rotate(${clue.bearing}deg)` }} />
      <span className="gg-visually-hidden">{` ${formatKm(clue.km)} km ${compassPoint(clue.bearing)}`}</span>
    </span>
  );
}
