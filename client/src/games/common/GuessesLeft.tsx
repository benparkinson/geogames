import { Circle, CircleFill } from "react-bootstrap-icons";

/** More allowed guesses than this are shown as text only, because a long row of dots is noise. */
const MAX_DOTS = 10;

/** How many wrong guesses the player has left. */
export default function GuessesLeft({ left, limit }: { left: number; limit: number }): React.JSX.Element {
  const last = left === 1;
  const label = last ? "1 guess left" : `${left} guesses left`;
  const text = last ? "Last guess!" : label;

  return (
    <div role="status" aria-label={label} className="guesses-left d-flex flex-wrap align-items-center gap-2">
      {limit <= MAX_DOTS && (
        <span className={"guesses-left-dots" + (last ? " gg-text-hint" : "")} aria-hidden="true">
          {Array.from({ length: limit }, (_, i) => (i < left ? <CircleFill key={i} /> : <Circle key={i} />))}
        </span>
      )}
      <span className={"small " + (last ? "gg-text-warning" : "gg-text-muted")}>{text}</span>
    </div>
  );
}
