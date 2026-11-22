import { CheckLg, EyeFill, StarFill, XLg } from "react-bootstrap-icons";
import { GameRoundView } from "../../api/types";
import { findGameModule } from "../registry";
import { CORRECT_ANSWER, OUT_OF_GUESSES } from "./Model";
import { roundOutcome } from "./outcome";

function roundLabel(round: GameRoundView): string {
  return findGameModule(round.gameType.name)?.singular ?? round.gameType.name;
}

/** The answers of a round, for the summary. */
function roundAnswers(round: GameRoundView): string[] {
  return findGameModule(round.gameType.name)?.answerNames(round.data) ?? [];
}

/** One round of the summary: its answers, how it went and its points. A button that opens the round. */
export default function SummaryRow({
  round,
  index,
  mixed,
  onOpen,
}: {
  round: GameRoundView;
  index: number;
  /** A mixed game names each round's type; a game of one type names it once, at the top. */
  mixed: boolean;
  onOpen: () => void;
}): React.JSX.Element {
  const outcome = roundOutcome(
    round.answerState,
    round.hintsUsed,
    round.gameResult.roundFirstGuess[index],
    round.points,
  );
  return (
    <button
      type="button"
      className="gg-list__item d-flex justify-content-between align-items-center gap-4 text-start"
      onClick={onOpen}
    >
      <span>
        {mixed ? (
          <>
            <span className="gg-text-muted">
              {index + 1}. {roundLabel(round)}
            </span>
            <br />
            {roundAnswers(round).join(", ")}
          </>
        ) : (
          <>
            <span className="gg-text-muted">{index + 1}.</span> {roundAnswers(round).join(", ")}
          </>
        )}
        {round.answerState === OUT_OF_GUESSES && (
          <>
            <br />
            <span className="small gg-text-muted">Out of guesses</span>
          </>
        )}
        {round.hintsUsed > 0 && (
          <>
            <br />
            <span className="small gg-text-muted">{round.hintsUsed === 1 ? "1 hint" : `${round.hintsUsed} hints`}</span>
          </>
        )}
      </span>
      <span className="text-nowrap" data-outcome={outcome ?? "missed"}>
        {outcome === "firstGuess" ? (
          <StarFill aria-hidden="true" />
        ) : round.answerState === CORRECT_ANSWER ? (
          <CheckLg aria-hidden="true" />
        ) : round.answerState === OUT_OF_GUESSES ? (
          <XLg aria-hidden="true" />
        ) : (
          <EyeFill aria-hidden="true" />
        )}{" "}
        +{round.points}
      </span>
    </button>
  );
}
