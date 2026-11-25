/** More lives than this are shown as text only, because a long row of boxes is noise. */
const MAX_BOXES = 10;

/** The player's lives as tally boxes: each wrong guess empties one, and the round ends when none are left. */
export default function GuessesLeft({ left, limit }: { left: number; limit: number }): React.JSX.Element {
  const last = left === 1;
  const label = last ? "1 life left" : `${left} lives left`;
  const text = last ? "Last life!" : `${left} lives left`;

  return (
    <div
      role="status"
      aria-label={label}
      title="Each wrong guess costs a life. You lose the round when you run out."
      className="guesses-left"
      data-last={last || undefined}
    >
      {limit <= MAX_BOXES && (
        <span className="guesses-left__boxes" aria-hidden="true">
          {Array.from({ length: limit }, (_, i) => (
            <span key={i} className="guesses-left__box" data-life={i < left ? "left" : "lost"} />
          ))}
        </span>
      )}
      <span className="guesses-left__text gg-mono" data-boxes={limit <= MAX_BOXES || undefined}>
        {text}
      </span>
    </div>
  );
}
