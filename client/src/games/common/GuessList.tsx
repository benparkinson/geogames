import { useEffect, useRef, useState } from "react";
import { useDistanceUnit } from "../../settings/units";
import { compassPoint, formatDistance } from "./distance";
import { GuessClue, GuessState, foundAnswers } from "./guessReducer";
import { GuessRow, guessListRows } from "./guessRows";
import { BearingIcon, FoundIcon, WrongGuessIcon } from "../../ui/icons";

/** The round's guesses as a ruled list: found answers, wrong guesses with their clue, and what is left. */
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
  const rows = guessListRows(state, allAnswers);
  // Rows already there on mount (a reload, a finished round) must not play the found animation.
  const [rowsAtMount] = useState(rows.length);
  const rowsRef = useRef<HTMLOListElement>(null);
  // Keep the newest guess in view when the list scrolls: after a new guess, and when the list is resized (a hint or the
  // keyboard takes some of its room).
  useEffect(() => {
    const list = rowsRef.current;
    if (!list) {
      return;
    }
    const showNewest = () => {
      list.scrollTop = list.scrollHeight;
    };
    showNewest();
    if (typeof ResizeObserver === "undefined") {
      return;
    }
    const observer = new ResizeObserver(showNewest);
    observer.observe(list);
    return () => observer.disconnect();
  }, [rows.length]);
  // With one answer there is nothing to count until a guess is made.
  if (rows.length === 0 && answerCount === 1) {
    return <></>;
  }
  const found = foundAnswers(state).length;

  return (
    <section className="guess-list" role="group" aria-label={`Answers: ${found} of ${answerCount} found`}>
      {rows.length > 0 && (
        <ol className="guess-list__rows" ref={rowsRef}>
          {rows.map((row, i) => (
            <li
              key={i}
              className="guess-list__row"
              data-kind={row.kind}
              data-outcome={row.kind === "missed" ? "missed" : undefined}
              data-fresh={row.kind === "found" && i >= rowsAtMount ? "true" : undefined}
              data-testid={row.kind === "wrong" ? "wrong-guess" : undefined}
            >
              <RowBody row={row} showClues={showClues} />
            </li>
          ))}
        </ol>
      )}
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
  }
}

/** The distance and an arrow towards the answer. */
function ClueBadge({ clue }: { clue: GuessClue }): React.JSX.Element {
  const [unit] = useDistanceUnit();
  const distance = formatDistance(clue.km, unit);
  return (
    <span className="guess-list__clue">
      {distance} <BearingIcon aria-hidden="true" style={{ transform: `rotate(${clue.bearing}deg)` }} />
      <span className="gg-visually-hidden">{` ${distance} ${compassPoint(clue.bearing)}`}</span>
    </span>
  );
}
