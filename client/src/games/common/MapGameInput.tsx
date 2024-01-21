import { GuessBox } from "../../components/GuessBox";
import { Round } from "./Model";
import { AnswerModel } from "./MapGame";
import { Button } from "react-bootstrap";
import { useState } from "react";
import Answer from "./Answer";

function MapGameInput({
  gameOver,
  round,
  answers,
  submitGuess
}: MapGameInputProps): JSX.Element {
  const [guess, setGuess] = useState("");

  function handleGuessChange(e: React.FormEvent<HTMLInputElement>) {
    setGuess(e.currentTarget.value);
  }

  function renderGuessBox(): JSX.Element {
    return (
      <GuessBox
        value={guess}
        name={'Guess'}
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
      <div className="col d-flex justify-content-center align-items-center">
        <div className="m-1 d-flex justify-content-center">
          <Button variant="primary" onClick={round.prevRound} disabled={!round.hasPreviousRound}>{"<"}</Button>
        </div>
        <div className="m-1 d-flex justify-content-center">
          <Button variant="primary" onClick={round.nextRound} disabled={nextRoundDisabled()}>{">"}</Button>
        </div>
      </div>
    );
  }

  function renderScoreDescription(): string {
    if (round.gameCompleted) {
      return "Final";
    } else {
      return "Current";
    }
  }

  function handleSubmitClick() {
    const guessSubmitted = submitGuess(guess);
    if (guessSubmitted) {
      setGuess("");
    }
  }

  return (
    <div id="input" className="row">
      <div id="new-game-div" className="d-flex align-items-center">
        <div className="m-1 d-flex justify-content-center">
          Round {round.currentRound + 1}/{round.totalRoundCount}
        </div>
        <div className="m-1 d-flex justify-content-center">
          {renderScoreDescription()} score: {round.score}/{round.totalRoundCount}
        </div>
        {renderRoundButtons()}
      </div>
      <div id="new-game-div" className="d-flex align-items-center justify-content-center">
        {renderGuessBox()}

        <div className="m-2 d-flex justify-content-center">
          <Button variant="success" onClick={handleSubmitClick}>Submit</Button>
        </div>
      </div>

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
}

export default MapGameInput;
