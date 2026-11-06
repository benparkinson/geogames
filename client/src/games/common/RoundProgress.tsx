import { CheckLg, EyeFill } from "react-bootstrap-icons";
import { CORRECT_ANSWER, GAVE_UP, UNANSWERED } from "./Model";

export default function RoundProgress({ states, currentRound, goToRound }: {
  states: string[];
  currentRound: number;
  goToRound: (round: number) => void;
}): JSX.Element {
  const firstUnanswered = states.findIndex(s => s === UNANSWERED);

  function canVisit(i: number): boolean {
    return i === currentRound || states[i] !== UNANSWERED || i === firstUnanswered;
  }

  function describe(i: number): string {
    const n = i + 1;
    if (i === currentRound) return `Round ${n}: current`;
    if (states[i] === CORRECT_ANSWER) return `Round ${n}: correct`;
    if (states[i] === GAVE_UP) return `Round ${n}: revealed`;
    return `Round ${n}: not played`;
  }

  function dotClass(i: number): string {
    const state = states[i];
    let cls = "round-dot";
    if (state === CORRECT_ANSWER) cls += " round-dot--correct";
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
              {state === CORRECT_ANSWER ? <CheckLg aria-hidden="true" />
                : state === GAVE_UP ? <EyeFill aria-hidden="true" />
                : i + 1}
            </button>
          </span>
        );
      })}
    </nav>
  );
}
