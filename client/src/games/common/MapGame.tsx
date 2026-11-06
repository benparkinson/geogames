import { useState } from "react";
import { normaliseString } from "../../helper/stringHelper";
import MapGameInput from "./MapGameInput";
import { Round, CORRECT_ANSWER, GAVE_UP, UNANSWERED } from "./Model";
import { Button, Stack } from "react-bootstrap";
import { Lightbulb } from "react-bootstrap-icons";
import MapGameScore from "./MapGameScore";

function MapGame<Type>({
  data,
  answerCount,
  MapComponent,
  correctAnswersFunction,
  round,
  prompt,
  inputPlaceholder,
  hints,
  submitAnswer,
  answerState,
  isAnswerCorrect
}: MapGameProps<Type>): JSX.Element {
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

  function renderCorrectAnswerChips() {
    return <div className="p-1 m-1 row">
      <Stack direction="horizontal" gap={1} className="mx-auto">
        {[...Array(answerCount)].map((_value: undefined, index: number) => <div key={index} className="mx-auto d-flex justify-content-center">
          <Button disabled variant={determineAnswerChipColour(index)} />
        </div>
        )}
      </Stack>
    </div>;
  }

  function determineAnswerChipColour(chipIndex: number) {
    const correctCount = attemptedGuesses.filter((answer: AnswerModel) => {
      return answer.correct;
    }).length;

    if (correctCount - 1 < chipIndex) {
      return "secondary";
    }

    return gaveUp ? "danger" : "success";
  }

  return (
    <div className="d-flex flex-column background">
      <div className="container flex-fill map-game">

        <Stack direction="horizontal" gap={3}>
          <div className="pt-1">
            <MapGameScore round={round} />
          </div>
        </Stack>

        <div className="map">{renderMap()}</div>

        {answerCount > 1 && renderCorrectAnswerChips()}

        <div className="m-1">
          <div>{prompt}</div>
          {hintTexts.map((text, index) => (
            <div key={index} className="small text-muted">
              <Lightbulb aria-hidden="true" /> {text}
            </div>
          ))}
        </div>

        <MapGameInput
          gameOver={gameOver}
          round={round}
          answers={attemptedGuesses}
          submitGuess={submitGuess}
          useHint={useHint}
          hintsUsed={hintsUsed}
          hints={hints}
          giveUp={giveUp}
          inputPlaceholder={inputPlaceholder}
        />
      </div>
    </div>
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
  | { kind: "wrong"; guess: string }
  | { kind: "correct"; remaining: number }
  | { kind: "over" };

export class MapGameProps<Type> {
  data: Type;
  answerCount: number;
  MapComponent: any;
  correctAnswersFunction: (data: Type) => string[];
  round: Round;
  prompt: string;
  inputPlaceholder: string;
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
