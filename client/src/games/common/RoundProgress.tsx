import { CheckLg, EyeFill, StarFill } from "react-bootstrap-icons";
import { CORRECT_ANSWER, GAVE_UP, UNANSWERED } from "./Model";
import { MAX_ROUND_POINTS } from "./scoring";

export default function RoundProgress({ states, points, currentRound, goToRound }: {
  states: string[];
  /** The points of each round, in the same order as `states`. */
  points: number[];
  currentRound: number;
  goToRound: (round: number) => void;
}): React.JSX.Element {
  const firstUnanswered = states.findIndex(s => s === UNANSWERED);

  function isPerfectRound(i: number): boolean {
    return states[i] === CORRECT_ANSWER && points[i] === MAX_ROUND_POINTS;
  }

  function canVisit(i: number): boolean {
    return i === currentRound || states[i] !== UNANSWERED || i === firstUnanswered;
  }

  function describe(i: number): string {
    const n = i + 1;
    if (i === currentRound) return `Round ${n}: current`;
    if (isPerfectRound(i)) return `Round ${n}: perfect`;
    if (states[i] === CORRECT_ANSWER) return `Round ${n}: correct, ${points[i]} points`;
    if (states[i] === GAVE_UP) return `Round ${n}: revealed`;
    return `Round ${n}: not played`;
  }

  function dotClass(i: number): string {
    const state = states[i];
    let cls = "round-dot";
    if (isPerfectRound(i)) cls += " round-dot--perfect";
    else if (state === CORRECT_ANSWER) cls += " round-dot--correct";
    else if (state === GAVE_UP) cls += " round-dot--revealed";
    if (i === currentRound) cls += " round-dot--current";
    return cls;
  }

  return (
    <nav aria-label="Rounds" className="round-progress">
      {states.map((state, i) => {
        const label = describe(i);
        const visitable = canVisit(i);
        return (
          <span key={i} title={visitable ? undefined : `Finish round ${i} first`}>
            <button
              type="button"
              className={dotClass(i)}
              aria-label={label}
              title={visitable ? label : undefined}
              aria-current={i === currentRound ? "step" : undefined}
              disabled={!visitable}
              onClick={() => goToRound(i)}
            >
              {isPerfectRound(i) ? <StarFill aria-hidden="true" />
                : state === CORRECT_ANSWER ? <CheckLg aria-hidden="true" />
                : state === GAVE_UP ? <EyeFill aria-hidden="true" />
                : i + 1}
            </button>
          </span>
        );
      })}
    </nav>
  );
}
