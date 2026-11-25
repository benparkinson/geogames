import GuessForm from "./GuessForm";
import GuessList from "./GuessList";
import { GuessResult, GuessState, foundAnswers, guessesLeft } from "./guessReducer";
import type { HintConfig } from "./hints";
import type { Round } from "./Model";
import RoundActions from "./RoundActions";
import RoundBrief from "./RoundBrief";
import RoundResult from "./RoundResult";
import RoundStatus from "./RoundStatus";
import type { RoundView } from "./roundView";
import type { Suggestion } from "./suggestions";
import { ContinentIcon, HintIcon } from "../../ui/icons";

export interface RoundPanelProps {
  round: Round;
  state: GuessState;
  view: RoundView;
  prompt: string;
  inputPlaceholder: string;
  answerNoun: string;
  suggestions?: Suggestion[];
  /** Every answer's name, shown in the slots when the round is revealed. */
  allAnswers: string[];
  hints: HintConfig;
  submitGuess: (guess: string) => GuessResult;
  giveUp: () => void;
  takeHint: () => void;
}

/** The panel beside the map: the brief, hints, the guess list, and either the guess form or the result. */
export default function RoundPanel({
  round,
  state,
  view,
  prompt,
  inputPlaceholder,
  answerNoun,
  suggestions,
  allAnswers,
  hints,
  submitGuess,
  giveUp,
  takeHint,
}: RoundPanelProps): React.JSX.Element {
  const guessList = (
    <GuessList
      state={state}
      answerCount={view.answerCount}
      allAnswers={allAnswers}
      showClues={round.difficulty !== "HARD"}
    />
  );
  const gameOver = state.over;
  return (
    <>
      <RoundBrief question={prompt} found={foundAnswers(state).length} answerCount={view.answerCount} />
      {(view.continentText !== null || view.hintTexts.length > 0) && (
        <ul className="round-hints" aria-label="Hints">
          {view.continentText !== null && (
            <li>
              <ContinentIcon aria-hidden="true" /> {view.continentText}
            </li>
          )}
          {view.hintTexts.map((text, index) => (
            <li key={index}>
              <HintIcon aria-hidden="true" /> {text}
            </li>
          ))}
        </ul>
      )}
      <div className="round-panel__body">
        {gameOver ? (
          <>
            {guessList}
            <RoundResult view={view} round={round} />
          </>
        ) : (
          <>
            {guessList}
            <RoundStatus
              guessesLeft={guessesLeft(state, round.guessLimit)}
              guessLimit={round.guessLimit}
              points={view.points}
            />
            <GuessForm
              submitGuess={submitGuess}
              inputPlaceholder={inputPlaceholder}
              answerNoun={answerNoun}
              suggestions={suggestions}
            />
            <RoundActions
              hints={hints}
              hintCount={round.hintCount}
              hintsUsed={view.hintsUsed}
              onHint={takeHint}
              onGiveUp={giveUp}
            />
          </>
        )}
      </div>
    </>
  );
}
