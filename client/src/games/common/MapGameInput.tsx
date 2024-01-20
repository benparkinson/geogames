import { GuessBox } from "../../components/GuessBox";
import { Round } from "./Model";
import { AnswerModel } from "./MapGame";
import { Button } from "react-bootstrap";
import { useState } from "react";
import Answer from "./Answer";

function MapGameInput({
  gameOver,
  round,
  clues,
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

  function renderRoundButtons(): JSX.Element {
    return (
      <div className="col d-flex justify-content-center align-items-center">
        <div className="m-1 d-flex justify-content-center">
          <Button variant="primary" onClick={round.prevRound} disabled={!round.hasPreviousRound}>Previous Round</Button>
        </div>
        <div className="m-1 d-flex justify-content-center">
          <Button variant="primary" onClick={round.nextRound} disabled={!round.hasNextRound}>Next Round</Button>
        </div>
      </div>
    );
  }

  function renderClues(): JSX.Element[] {
    if (clues) {
      return clues.map((clue, index) => (
        <div key={index} className="m-1">
          {clue}
        </div>
      ));
    }
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
    <div id="input" className="row align-items-center">
      <div id="new-game-div" className="d-flex justify-content-center align-items-center">
        <div className="m-1 d-flex justify-content-center">
          Round {round.currentRound + 1}/{round.totalRoundCount}
        </div>
        <div className="m-1 d-flex justify-content-center">
          {renderScoreDescription()} score: {round.score}/{round.totalRoundCount}
        </div>
        {renderRoundButtons()}
      </div>
      <div className="col justify-content-center align-items-center">
        {renderGuessBox()}
      </div>

      <div className="col justify-content-center align-items-center">
        <div className="m-1 d-flex justify-content-center">
          <Button variant="success" onClick={handleSubmitClick}>Submit</Button>
        </div>
      </div>

      <div className="col justify-content-center align-items-center">
        <div className="m-1 d-flex justify-content-center">
          {renderClues()}
        </div>
      </div>
      <div className="m-1 d-flex">
        {renderAnswers()}
      </div>
    </div>
  );
}

export class MapGameInputProps {
  gameOver: boolean;
  round: Round;
  clues?: string[];
  answers: AnswerModel[];
  submitGuess: (guess: string) => boolean;
}

export default MapGameInput;
