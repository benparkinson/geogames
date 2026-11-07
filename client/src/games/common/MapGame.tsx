import { useState } from "react";
import MapGameInput from "./MapGameInput";
import { AnswerState, Round } from "./Model";
import { Lightbulb } from "react-bootstrap-icons";
import GameLayout from "./GameLayout";
import GameTopBar from "./GameTopBar";
import AnswerSlots from "./AnswerSlots";
import { MapProps } from "../../map/MapProps";
import { AnswerCandidate } from "./answers";
import { GuessAction, guessReducer, GuessState, initialGuessState } from "./guessReducer";

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
  mapCaption,
  suggestions,
  isKnownGuess,
  hints,
  submitAnswer,
}: MapGameProps<Type>): React.JSX.Element {
  const [guessState, setGuessState] = useState<GuessState>(() => initialGuessState(round.answerState, answers));
  const [hintTexts, setHintTexts] = useState<string[]>([]);
  const { guesses, gaveUp, over: gameOver, hintsUsed } = guessState;

  function dispatch(action: GuessAction) {
    const outcome = guessReducer(guessState, action, {
      answers,
      answerCount,
      isKnownGuess,
      hintsTotal: hints?.total ?? 0,
    });
    setGuessState(outcome.state);
    if (outcome.finished) {
      submitAnswer(outcome.finished);
    }
    return outcome;
  }

  function submitGuess(guess: string) {
    return dispatch({ type: "guess", text: guess }).result ?? { kind: "over" as const };
  }

  function giveUp() {
    dispatch({ type: "giveUp" });
  }

  function takeHint() {
    if (gameOver || !hints || hintsUsed >= hints.total) {
      return;
    }

    const text = hints.text?.(hintsUsed);
    if (text) {
      setHintTexts([...hintTexts, text]);
    }
    dispatch({ type: "hint" });
  }

  const foundAnswers = guesses.flatMap(g => (g.answer === null ? [] : [g.answer]));
  const showBlankMap = !!blankMap && !gameOver;
  const showCaption = !!mapCaption && !gameOver;

  const map = (
    <>
      <MapComponent data={data} gameOver={gameOver} gaveUp={gaveUp} hintsUsed={hintsUsed} />
      {showCaption && <div className="map-caption">{mapCaption}</div>}
    </>
  );

  const panel = (
    <>
      <div className="mb-2">{prompt}</div>
      {hintTexts.map((text, index) => (
        <div key={index} className="small text-muted mb-1">
          <Lightbulb aria-hidden="true" /> {text}
        </div>
      ))}
      <AnswerSlots
        answerCount={answerCount}
        found={foundAnswers}
        gaveUp={gaveUp}
        allAnswers={gaveUp ? foundAnswers : []}
      />
      <MapGameInput
        gameOver={gameOver}
        gaveUp={gaveUp}
        round={round}
        wrongGuesses={guesses.filter(g => g.answer === null)}
        submitGuess={submitGuess}
        onHint={takeHint}
        hintsUsed={hintsUsed}
        hints={hints}
        giveUp={giveUp}
        inputPlaceholder={inputPlaceholder}
        answerNoun={answerNoun}
        suggestions={suggestions}
      />
    </>
  );

  return (
    <GameLayout
      topBar={<GameTopBar round={round} gameOver={gameOver} gaveUp={gaveUp} />}
      map={map}
      panel={panel}
      mapClassName={showBlankMap ? "game-map--blank" : undefined}
    />
  );
}

export interface HintConfig {
  total: number;
  description: string;
  /** Text to show for the hint at this index. Leave it out for hints that only change the map. */
  text?: (hintIndex: number) => string | null;
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
  /** A caption over the map while the round is being played. */
  mapCaption?: string;
  suggestions?: string[];
  isKnownGuess?: (guess: string) => boolean;
  hints?: HintConfig;
  submitAnswer: (answerState: AnswerState) => void;
}

export default MapGame;
