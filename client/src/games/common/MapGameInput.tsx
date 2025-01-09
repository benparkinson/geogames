import { GuessBox } from "../../components/GuessBox";
import { Round } from "./Model";
import { AnswerModel } from "./MapGame";
import { Button, Stack } from "react-bootstrap";
import { useState } from "react";
import Answer from "./Answer";

function MapGameInput({
  gameOver,
  round,
  answers,
  submitGuess,
  displayMapHint
}: MapGameInputProps): JSX.Element {
  const [guess, setGuess] = useState("");

  function handleGuessChange(e: React.FormEvent<HTMLInputElement>) {
    setGuess(e.currentTarget.value);
  }

  function renderGuessBox(): JSX.Element {
    return (
      <GuessBox
        value={guess}
        name={'Guess a country...'}
        onChange={handleGuessChange}
        correct={null}
        disabled={gameOver}
        handleEnterKey={handleSubmitClick}
      />
    );
  }

  function renderAnswers(): JSX.Element[] {
    return answers.map((answer, index) => (
      <div className="m-1" key={index}>
        <Answer answer={answer.answer} isCorrect={answer.correct} />
      </div>
    ));
  }

  function nextRoundDisabled(): boolean {
    if (!round.hasNextRound) {
      return true;
    }
    return !gameOver;
  }

  function renderRoundButtons(): JSX.Element {
    return (
      <div className="d-flex ms-auto">
        <div className="m-1 d-flex justify-content-center">
          <Button variant="light" onClick={round.prevRound} disabled={!round.hasPreviousRound} >{"⏮️"}</Button>
        </div>
        <div className="m-1 d-flex justify-content-center">
          <Button variant="light" onClick={round.nextRound} disabled={nextRoundDisabled()}>{"⏭️"}</Button>
        </div>
      </div>
    );
  }

  function handleSubmitClick() {
    const guessSubmitted = submitGuess(guess);
    if (guessSubmitted) {
      setGuess("");
    }
  }

  function handleClueClick() {
    if (displayMapHint && !gameOver) {
      displayMapHint();
    }
  }

  return (
    <div id="input" className="row">
      <Stack direction="horizontal" gap={1} className="mx-auto">
        {renderGuessBox()}

        <div className="m-1 d-flex justify-content-center">
          <Button variant="info" onClick={handleSubmitClick}>🌍</Button>

        </div>
        <div className="m-1 d-flex justify-content-center">

          <Button variant="warning" onClick={handleClueClick}>💡</Button>
        </div>
        {renderRoundButtons()}
      </Stack>

      <div className="m-1 d-flex flex-wrap">
        {renderAnswers()}
      </div>
    </div>
  );
}

export class MapGameInputProps {
  gameOver: boolean;
  round: Round;
  answers: AnswerModel[];
  submitGuess: (guess: string) => boolean;
  displayMapHint?: () => void;
}

export default MapGameInput;
