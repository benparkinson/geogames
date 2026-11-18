import { ArrowUp, XLg } from "react-bootstrap-icons";
import { Chip } from "../../ui";
import { compassPoint, formatKm } from "./distance";
import { Guess, GuessClue } from "./guessReducer";

/** The wrong guesses so far, each with its distance clue unless Hard mode hides them. */
export default function WrongGuessList({
  guesses,
  showClues,
}: {
  guesses: Guess[];
  showClues: boolean;
}): React.JSX.Element {
  return (
    <div>
      <div className="gg-visually-hidden">Wrong guesses</div>
      <div className="d-flex flex-wrap gap-1">
        {guesses.map((a, i) => (
          <Chip key={i} tone="neutral" className="guess-chip">
            <XLg aria-hidden="true" className="guess-chip__x" /> {a.text}
            {showClues && a.clue && <ClueBadge clue={a.clue} />}
          </Chip>
        ))}
      </div>
    </div>
  );
}

/** The distance and an arrow towards the answer, after a wrong guess's name. */
function ClueBadge({ clue }: { clue: GuessClue }): React.JSX.Element {
  return (
    <span className="ms-2 fw-semibold">
      {formatKm(clue.km)} km <ArrowUp aria-hidden="true" style={{ transform: `rotate(${clue.bearing}deg)` }} />
      <span className="gg-visually-hidden">{` ${formatKm(clue.km)} km ${compassPoint(clue.bearing)}`}</span>
    </span>
  );
}
