import { CORRECT_ANSWER, OUT_OF_GUESSES, UNANSWERED } from "./Model";
import { RoundOutcome } from "./outcome";
import { RESULT_ICONS } from "../../ui/icons";

export default function RoundProgress({
  states,
  points,
  outcomes,
  currentRound,
  goToRound,
}: {
  states: string[];
  /** The points of each round, in the same order as `states`. */
  points: number[];
  /** How each round ended (see `roundOutcome`), null while unanswered, in the same order as `states`. */
  outcomes: (RoundOutcome | null)[];
  currentRound: number;
  goToRound: (round: number) => void;
}): React.JSX.Element {
  const firstUnanswered = states.findIndex((s) => s === UNANSWERED);

  function canVisit(i: number): boolean {
    return i === currentRound || states[i] !== UNANSWERED || i === firstUnanswered;
  }

  function describe(i: number): string {
    const n = i + 1;
    if (i === currentRound) return `Round ${n}: current`;
    const revealed = states[i] === OUT_OF_GUESSES ? "out of guesses" : "revealed";
    switch (outcomes[i]) {
      case "firstGuess":
        return `Round ${n}: first guess`;
      case "correct":
        return `Round ${n}: correct`;
      case "hints":
        return states[i] === CORRECT_ANSWER
          ? `Round ${n}: correct with hints, ${points[i]} points`
          : `Round ${n}: ${revealed}, ${points[i]} points`;
      case "missed":
        return `Round ${n}: ${revealed}`;
      default:
        return `Round ${n}: not played`;
    }
  }

  /** Which result's glyph the dot shows; null while unanswered, when it shows the round's number. */
  function result(i: number): keyof typeof RESULT_ICONS | null {
    const outcome = outcomes[i];
    if (outcome === null || outcome === undefined) return null;
    if (outcome === "firstGuess") return "firstGuess";
    if (states[i] === CORRECT_ANSWER) return "correct";
    return states[i] === OUT_OF_GUESSES ? "outOfGuesses" : "revealed";
  }

  function icon(i: number): React.ReactNode {
    const shown = result(i);
    if (shown === null) return i + 1;
    const Icon = RESULT_ICONS[shown];
    return <Icon aria-hidden="true" />;
  }

  return (
    <nav aria-label="Rounds" className="round-progress">
      {states.map((_state, i) => {
        const label = describe(i);
        const visitable = canVisit(i);
        return (
          <span key={i} title={visitable ? undefined : `Finish round ${i} first`}>
            <button
              type="button"
              className="round-dot"
              data-outcome={outcomes[i] ?? undefined}
              data-result={result(i) ?? undefined}
              aria-label={label}
              title={visitable ? label : undefined}
              aria-current={i === currentRound ? "step" : undefined}
              disabled={!visitable}
              onClick={() => goToRound(i)}
            >
              {icon(i)}
            </button>
          </span>
        );
      })}
    </nav>
  );
}
