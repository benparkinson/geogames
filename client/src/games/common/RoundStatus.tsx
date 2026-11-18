import GuessesLeft from "./GuessesLeft";

/** One row above the input: the guesses left on the left, what the round is worth now on the right. */
export default function RoundStatus({ guessesLeft, guessLimit, points }: {
  guessesLeft: number;
  guessLimit: number;
  points: number;
}): React.JSX.Element {
  return (
    <div className="round-status d-flex justify-content-between align-items-center mb-2">
      <GuessesLeft left={guessesLeft} limit={guessLimit} />
      <span className="small gg-text-muted ms-auto">{`Worth ${points} pts`}</span>
    </div>
  );
}
