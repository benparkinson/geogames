import GuessesLeft from "./GuessesLeft";

/** One row between the guess list and the input: the lives left on the left, what the round is worth now on the right. */
export default function RoundStatus({
  guessesLeft,
  guessLimit,
  points,
}: {
  guessesLeft: number;
  guessLimit: number;
  /** What the round is worth now (`RoundView.points` while playing). */
  points: number;
}): React.JSX.Element {
  return (
    <div data-testid="round-status" className="round-status">
      <GuessesLeft left={guessesLeft} limit={guessLimit} />
      <span className="round-status__points gg-mono gg-text-muted">{`Worth ${points}`}</span>
    </div>
  );
}
