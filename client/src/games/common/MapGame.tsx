import { Lightbulb } from "react-bootstrap-icons";
import { MapProps } from "../../map/MapProps";
import { AnswerCandidate } from "./answers";
import AnswerSlots from "./AnswerSlots";
import GameLayout from "./GameLayout";
import GameTopBar from "./GameTopBar";
import GuessForm from "./GuessForm";
import { foundAnswers, GuessClue, guessesLeft, wrongGuesses } from "./guessReducer";
import type { HintConfig } from "./hints";
import { CORRECT_ANSWER, Round, RoundAnswer } from "./Model";
import RoundActions from "./RoundActions";
import RoundBrief from "./RoundBrief";
import RoundResult from "./RoundResult";
import RoundStatus from "./RoundStatus";
import { roundPoints } from "./scoring";
import { useRoundGuesses } from "./useRoundGuesses";
import WrongGuessList from "./WrongGuessList";

function MapGame<Type>({
  data,
  answerCount,
  MapComponent,
  answers,
  round,
  prompt,
  inputPlaceholder,
  answerNoun,
  blankMap,
  suggestions,
  isKnownGuess,
  hints,
  clueFor,
  submitAnswer,
}: MapGameProps<Type>): React.JSX.Element {
  const { state, submitGuess, giveUp, takeHint } = useRoundGuesses({
    round,
    answers,
    answerCount,
    isKnownGuess,
    clueFor,
    submitAnswer,
  });
  const { gaveUp, outOfGuesses, over: gameOver, hintsUsed } = state;
  const wrong = wrongGuesses(state);

  // Derived, so a reopened round shows the hints that were used.
  const hintTexts = Array.from({ length: hintsUsed }, (_, i) => hints.text?.(i, state.foundAtHint)).filter(
    (text): text is string => !!text,
  );

  const map = (
    <>
      <MapComponent
        data={data}
        gameOver={gameOver}
        gaveUp={gaveUp}
        hintsUsed={hintsUsed}
        foundAtHint={state.foundAtHint}
      />
    </>
  );

  const panel = (
    <>
      <RoundBrief title={round.kind.title} Icon={round.kind.Icon} question={prompt} />
      {hintTexts.length > 0 && (
        <ul className="round-hints">
          {hintTexts.map((text, index) => (
            <li key={index}>
              <Lightbulb aria-hidden="true" /> {text}
            </li>
          ))}
        </ul>
      )}
      <AnswerSlots
        answerCount={answerCount}
        found={foundAnswers(state)}
        gaveUp={gaveUp}
        allAnswers={answers.map((a) => a.name)}
      />
      <div>
        {gameOver ? (
          <>
            {wrong.length > 0 && <WrongGuessList guesses={wrong} showClues={!round.hardMode} />}
            <RoundResult
              gaveUp={gaveUp}
              outOfGuesses={outOfGuesses}
              round={round}
              hintsUsed={hintsUsed}
              hintCount={round.hintCount}
              answersFound={foundAnswers(state).length}
              answerCount={answerCount}
            />
          </>
        ) : (
          <>
            <RoundStatus
              guessesLeft={guessesLeft(state, round.guessLimit)}
              guessLimit={round.guessLimit}
              points={roundPoints(CORRECT_ANSWER, hintsUsed, round.hintCount)}
            />
            {wrong.length > 0 && <WrongGuessList guesses={wrong} showClues={!round.hardMode} />}
            <GuessForm
              submitGuess={submitGuess}
              inputPlaceholder={inputPlaceholder}
              answerNoun={answerNoun}
              suggestions={suggestions}
            />
            <RoundActions
              hints={hints}
              hintCount={round.hintCount}
              hintsUsed={hintsUsed}
              onHint={takeHint}
              onGiveUp={giveUp}
            />
          </>
        )}
      </div>
    </>
  );

  return (
    <GameLayout
      topBar={
        <GameTopBar
          round={round}
          gameOver={gameOver}
          gaveUp={gaveUp}
          outOfGuesses={outOfGuesses}
          hintsUsed={hintsUsed}
          answersFound={foundAnswers(state).length}
          answerCount={answerCount}
        />
      }
      map={map}
      panel={panel}
      mapClassName={blankMap && !gameOver ? "game-map--blank" : undefined}
    />
  );
}

export interface MapGameProps<Type> {
  data: Type;
  answerCount: number;
  MapComponent: React.ComponentType<MapProps<Type>>;
  /** The answers to find, with the aliases that also count. */
  answers: AnswerCandidate[];
  round: Round;
  prompt: string;
  inputPlaceholder: string;
  /** What the player is naming, singular: used in "We don't recognise "x" as a country". */
  answerNoun: string;
  /** Shows a grey map instead of the real one while the round is being played. */
  blankMap?: boolean;
  suggestions?: string[];
  isKnownGuess?: (guess: string) => boolean;
  /** What the round's hints say. The round's `hintCount` (from the backend) says how many there are. */
  hints: HintConfig;
  /**
   * Gives a wrong guess a clue (distance and direction to the nearest answer not found yet). Leave it
   * out for a game whose map already shows where the answer is. Hard mode hides the clues.
   */
  clueFor?: (guess: string, targets: string[]) => GuessClue | null;
  submitAnswer: (answer: RoundAnswer) => void;
}

export default MapGame;
