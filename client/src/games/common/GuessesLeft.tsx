import { Circle, CircleFill } from "react-bootstrap-icons";

/** More allowed guesses than this are shown as text only, because a long row of dots is noise. */
const MAX_DOTS = 10;

/** How many wrong guesses the player has left. Renders nothing when there is no limit. */
export default function GuessesLeft({ left, limit }: { left: number; limit: number }): React.JSX.Element | null {
  if (!Number.isFinite(limit)) {
    return null;
  }
  const last = left === 1;
  const text = last ? "1 guess left" : `${left} guesses left`;

  return (
    <div role="status" aria-label={text} className="guesses-left d-flex flex-wrap align-items-center gap-2 mb-1">
      {limit <= MAX_DOTS && (
        <span className={"guesses-left-dots" + (last ? " gg-text-hint" : "")} aria-hidden="true">
          {Array.from({ length: limit }, (_, i) => (i < left ? <CircleFill key={i} /> : <Circle key={i} />))}
        </span>
      )}
      <span className={"small " + (last ? "gg-text-warning" : "gg-text-muted")}>{text}</span>
    </div>
  );
}
