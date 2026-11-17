import { useEffect, useState } from "react";
import MapGameInput from "./MapGameInput";
import { Round, RoundAnswer } from "./Model";
import { Lightbulb } from "react-bootstrap-icons";
import GameLayout from "./GameLayout";
import GameTopBar from "./GameTopBar";
import AnswerSlots from "./AnswerSlots";
import { MapProps } from "../../map/MapProps";
import { AnswerCandidate } from "./answers";
import HardcoreToggle from "../../components/HardcoreToggle";
import { useHardcoreMode } from "../../settings/hardcoreMode";
import { GuessAction, GuessClue, guessesLeft, guessReducer, GuessState, initialGuessState, wrongGuessCount } from "./guessReducer";

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
  clueFor,
  submitAnswer,
}: MapGameProps<Type>): React.JSX.Element {
  const [guessState, setGuessState] = useState<GuessState>(() =>
    initialGuessState(round.answerState, answers, round.hintsUsed));
  const { guesses, gaveUp, outOfGuesses, over: gameOver, hintsUsed } = guessState;
  // The backend decides how many hints there are; the game only supplies what they say.
  const [hardcore] = useHardcoreMode();
  const hintCount = round.hintCount;
  const hintConfig = hintCount > 0 ? hints : undefined;

  useEffect(() => {
    if (hintCount > 0 && !hints && process.env.NODE_ENV === "development") {
      console.error(`This game type has ${hintCount} hints but its component supplies no hints.`);
    }
  }, [hintCount, hints]);

  // Derived, so a reopened round shows the hints that were used.
  const hintTexts = Array.from({ length: hintsUsed }, (_, i) => hintConfig?.text?.(i))
    .filter((text): text is string => !!text);

  function dispatch(action: GuessAction) {
    const outcome = guessReducer(guessState, action, {
      answers,
      answerCount,
      isKnownGuess,
      hintsTotal: hintConfig ? hintCount : 0,
      guessLimit: round.guessLimit,
      clueFor,
    });
    setGuessState(outcome.state);
    if (outcome.finished) {
      submitAnswer({
        answerState: outcome.finished,
        hintsUsed: outcome.state.hintsUsed,
        wrongGuesses: wrongGuessCount(outcome.state),
      });
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
    if (gameOver || !hintConfig || hintsUsed >= hintCount) {
      return;
    }
    dispatch({ type: "hint" });
  }

  const foundAnswers = guesses.flatMap(g => (g.answer === null ? [] : [g.answer]));
  const showBlankMap = !!blankMap && !gameOver;
  const showCaption = !!mapCaption && !gameOver;

  const map = (
    <>
      <MapComponent data={data} gameOver={gameOver} gaveUp={gaveUp} hintsUsed={hintsUsed} foundAtHint={guessState.foundAtHint} />
      {showCaption && <div className="map-caption">{mapCaption}</div>}
    </>
  );

  const panel = (
    <>
      <div className="mb-2">{prompt}</div>
      {hintTexts.map((text, index) => (
        <div key={index} className="small gg-text-muted mb-1">
          <Lightbulb aria-hidden="true" /> {text}
        </div>
      ))}
      <AnswerSlots
        answerCount={answerCount}
        found={foundAnswers}
        gaveUp={gaveUp}
        allAnswers={answers.map(a => a.name)}
      />
      <MapGameInput
        gameOver={gameOver}
        gaveUp={gaveUp}
        outOfGuesses={outOfGuesses}
        guessesLeft={guessesLeft(guessState, round.guessLimit)}
        round={round}
        wrongGuesses={guesses.filter(g => g.answer === null)}
        submitGuess={submitGuess}
        onHint={takeHint}
        hintsUsed={hintsUsed}
        hintCount={hintConfig ? hintCount : 0}
        hints={hintConfig}
        giveUp={giveUp}
        inputPlaceholder={inputPlaceholder}
        answerNoun={answerNoun}
        answerCount={answerCount}
        showClues={!hardcore}
        suggestions={suggestions}
      />
      {clueFor && !gameOver && (
        <div className="mt-2">
          <HardcoreToggle compact />
        </div>
      )}
    </>
  );

  return (
    <GameLayout
      topBar={<GameTopBar round={round} gameOver={gameOver} gaveUp={gaveUp} outOfGuesses={outOfGuesses} hintsUsed={hintsUsed} />}
      map={map}
      panel={panel}
      mapClassName={showBlankMap ? "game-map--blank" : undefined}
    />
  );
}

/** What a game's hints say. How many there are comes from the backend: `round.hintCount`. */
export interface HintConfig {
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
  /**
   * Gives a wrong guess a clue (distance and direction to the nearest answer not found yet). Leave it
   * out for a game whose map already shows where the answer is. Hardcore mode hides the clues.
   */
  clueFor?: (guess: string, targets: string[]) => GuessClue | null;
  submitAnswer: (answer: RoundAnswer) => void;
}

export default MapGame;
