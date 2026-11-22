import AnswerSlots from "./AnswerSlots";
import GuessForm from "./GuessForm";
import { foundAnswers, GuessResult, GuessState, guessesLeft, wrongGuesses } from "./guessReducer";
import type { HintConfig } from "./hints";
import type { Round } from "./Model";
import RoundActions from "./RoundActions";
import RoundBrief from "./RoundBrief";
import RoundResult from "./RoundResult";
import RoundStatus from "./RoundStatus";
import type { RoundView } from "./roundView";
import type { Suggestion } from "./suggestions";
import WrongGuessList from "./WrongGuessList";
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

/** The panel beside the map: the brief, hints, answer slots, and either the guess form or the result. */
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
  const wrong = wrongGuesses(state);
  const gameOver = state.over;
  return (
    <>
      <RoundBrief title={round.kind.title} Icon={round.kind.Icon} question={prompt} />
      {(view.continentText !== null || view.hintTexts.length > 0) && (
        <ul className="round-hints">
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
      <AnswerSlots
        answerCount={view.answerCount}
        found={foundAnswers(state)}
        gaveUp={state.gaveUp}
        allAnswers={allAnswers}
      />
      <div>
        {gameOver ? (
          <>
            {wrong.length > 0 && <WrongGuessList guesses={wrong} showClues={round.difficulty !== "HARD"} />}
            <RoundResult view={view} round={round} />
          </>
        ) : (
          <>
            <RoundStatus
              guessesLeft={guessesLeft(state, round.guessLimit)}
              guessLimit={round.guessLimit}
              points={view.points}
            />
            {wrong.length > 0 && <WrongGuessList guesses={wrong} showClues={round.difficulty !== "HARD"} />}
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
