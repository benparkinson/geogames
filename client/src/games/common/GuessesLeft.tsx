import { LifeLostIcon, LifeIcon } from "../../ui/icons";

/** More lives than this are shown as text only, because a long row of hearts is noise. */
const MAX_HEARTS = 10;

/** The player's lives: each wrong guess costs one, and the round ends when none are left. */
export default function GuessesLeft({ left, limit }: { left: number; limit: number }): React.JSX.Element {
  const last = left === 1;
  const label = last ? "1 life left" : `${left} lives left`;
  const text = last ? "Last life!" : `${left} of ${limit} lives left`;

  return (
    <div
      role="status"
      aria-label={label}
      title="Each wrong guess costs a life. You lose the round when you run out."
      className="guesses-left d-flex flex-wrap align-items-center gap-2"
    >
      {limit <= MAX_HEARTS && (
        <span className={"guesses-left-dots" + (last ? " gg-text-hint" : "")} aria-hidden="true">
          {Array.from({ length: limit }, (_, i) => (i < left ? <LifeIcon key={i} /> : <LifeLostIcon key={i} />))}
        </span>
      )}
      <span className={"small " + (last ? "gg-text-warning" : "gg-text-muted")}>{text}</span>
    </div>
  );
}
