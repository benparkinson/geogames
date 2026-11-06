import { useState } from "react";
import { normaliseString } from "../../helper/stringHelper";
import MapGameInput from "./MapGameInput";
import { Round, CORRECT_ANSWER, GAVE_UP, UNANSWERED } from "./Model";
import { Lightbulb } from "react-bootstrap-icons";
import GameLayout from "./GameLayout";
import GameTopBar from "./GameTopBar";
import AnswerSlots from "./AnswerSlots";
import { MapProps } from "../../map/MapProps";

function MapGame<Type>({
  data,
  answerCount,
  MapComponent,
  correctAnswersFunction,
  round,
  prompt,
  inputPlaceholder,
  blankMapCaption,
  suggestions,
  isKnownGuess,
  hints,
  submitAnswer,
  answerState,
  isAnswerCorrect
}: MapGameProps<Type>): React.JSX.Element {
  const [attemptedGuesses, setAttemptedGuesses] = useState<AnswerModel[]>(() =>
    answerState !== UNANSWERED
      ? correctAnswersFunction(data).map(a => new AnswerModel(a, true))
      : []
  );
  const [gaveUp, setGaveUp] = useState(answerState === GAVE_UP);
  const [gameOver, setGameOver] = useState(answerState !== UNANSWERED);
  const [hintsUsed, setHintsUsed] = useState(0);
  const [hintTexts, setHintTexts] = useState<string[]>([]);

  function renderMap() {
    return <MapComponent data={data} gameOver={gameOver} gaveUp={gaveUp} />;
  }

  function submitGuess(guess: string): GuessResult {
    if (gameOver) {
      return { kind: "over" };
    }

    const trimmed = guess.trim();
    if (trimmed === "") {
      return { kind: "empty" };
    }

    const allGuesses = attemptedGuesses.map((answer: AnswerModel) => normaliseString(answer.answer));
    if (allGuesses.includes(normaliseString(trimmed))) {
      return { kind: "duplicate", guess: trimmed };
    }

    if (isKnownGuess && !isKnownGuess(trimmed)) {
      return { kind: "unknown", guess: trimmed };
    }

    const correct = isAnswerCorrect(trimmed, data);
    const newAttemptedGuesses = [...attemptedGuesses, new AnswerModel(trimmed, correct)];
    const correctCount = newAttemptedGuesses.filter(answer => answer.correct).length;

    setAttemptedGuesses(newAttemptedGuesses);

    if (!correct) {
      return { kind: "wrong", guess: trimmed };
    }

    const remaining = answerCount - correctCount;
    if (remaining <= 0) {
      submitAnswer(CORRECT_ANSWER);
      setGameOver(true);
      return { kind: "correct", remaining: 0 };
    }
    return { kind: "correct", remaining };
  }

  function giveUp() {
    if (gameOver) {
      return;
    }

    setAttemptedGuesses(correctAnswersFunction(data).map(a => new AnswerModel(a, true)));
    submitAnswer(GAVE_UP);
    setGaveUp(true);
    setGameOver(true);
  }

  function useHint() {
    if (gameOver || !hints || hintsUsed >= hints.total) {
      return;
    }

    const text = hints.use(hintsUsed);
    if (text) {
      setHintTexts([...hintTexts, text]);
    }
    setHintsUsed(hintsUsed + 1);
  }

  const foundAnswers = attemptedGuesses.filter(a => a.correct).map(a => a.answer);
  const showBlankMap = !!blankMapCaption && !gameOver;

  const map = (
    <>
      {renderMap()}
      {showBlankMap && <div className="map-caption">{blankMapCaption}</div>}
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
        allAnswers={gaveUp ? attemptedGuesses.map(a => a.answer) : []}
      />
      <MapGameInput
        gameOver={gameOver}
        gaveUp={gaveUp}
        round={round}
        wrongGuesses={attemptedGuesses.filter(a => !a.correct)}
        submitGuess={submitGuess}
        useHint={useHint}
        hintsUsed={hintsUsed}
        hints={hints}
        giveUp={giveUp}
        inputPlaceholder={inputPlaceholder}
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
  use: (hintIndex: number) => string | null;
}

export type GuessResult =
  | { kind: "empty" }
  | { kind: "duplicate"; guess: string }
  | { kind: "unknown"; guess: string }
  | { kind: "wrong"; guess: string }
  | { kind: "correct"; remaining: number }
  | { kind: "over" };

export class MapGameProps<Type> {
  data: Type;
  answerCount: number;
  MapComponent: React.ComponentType<MapProps<Type>>;
  correctAnswersFunction: (data: Type) => string[];
  round: Round;
  prompt: string;
  inputPlaceholder: string;
  blankMapCaption?: string;
  suggestions?: string[];
  isKnownGuess?: (guess: string) => boolean;
  hints?: HintConfig;
  submitAnswer: (answerState: string) => void;
  answerState: string;
  isAnswerCorrect: (guess: string, data: Type) => boolean;
}

export class AnswerModel {
  answer: string;
  correct: boolean;

  constructor(answer: string, correct: boolean) {
    this.answer = answer;
    this.correct = correct;
  }
}

export default MapGame;
