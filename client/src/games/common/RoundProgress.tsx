import { UNANSWERED } from "./Model";
import { RoundOutcome } from "./outcome";

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
    if (outcomes[i] == null) return `Round ${n}: not played`;
    return `Round ${n}: ${points[i]} ${points[i] === 1 ? "point" : "points"}`;
  }

  return (
    <nav aria-label="Rounds" className="round-progress">
      {states.map((_state, i) => {
        const label = describe(i);
        const visitable = canVisit(i);
        const outcome = outcomes[i];
        return (
          <span key={i} title={visitable ? undefined : `Finish round ${i} first`}>
            <button
              type="button"
              className="round-dot"
              data-outcome={outcome ?? undefined}
              aria-label={label}
              title={visitable ? label : undefined}
              aria-current={i === currentRound ? "step" : undefined}
              disabled={!visitable}
              onClick={() => goToRound(i)}
            >
              {outcome == null ? i + 1 : points[i]}
            </button>
          </span>
        );
      })}
    </nav>
  );
}
