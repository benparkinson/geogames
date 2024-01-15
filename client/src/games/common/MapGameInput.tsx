import { GuessBox } from "../../components/GuessBox";
import { Button } from "../../components/Button";
import { Round } from "./Model";
import { Modal } from "react-bootstrap";
import { useState } from "react";

function MapGameInput({
  guesses,
  correctnessArray,
  guessBoxName,
  gaveUp,
  handleGuessInput,
  submitGuesses,
  round,
  clues
}: MapGameInputProps): JSX.Element {

  function renderBoxes(): JSX.Element[] {
    return guesses.map((g, index) => (
      <GuessBox
        key={index}
        value={guesses[index]}
        name={guessBoxName(index)}
        onChange={handleGuessInput}
        correct={correctnessArray[index]}
        disabled={gaveUp}
      />
    ));
  }

  function renderRoundButtons(): JSX.Element {
    return (
      <div className="col d-flex justify-content-center align-items-center">
        <div className="m-1 d-flex justify-content-center">
          <Button bootstrapClass="btn-primary" text={"Previous Round"} onClick={round.prevRound} disabled={!round.hasPreviousRound} />
        </div>
        <div className="m-1 d-flex justify-content-center">
          <Button bootstrapClass="btn-primary" text={"Next Round"} onClick={round.nextRound} disabled={!round.hasNextRound} />
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
        {renderBoxes()}
      </div>

      <div className="col justify-content-center align-items-center">
        <div className="m-1 d-flex justify-content-center">
          <Button bootstrapClass="btn-success" text={"Submit"} onClick={submitGuesses} />
        </div>
      </div>

      <div className="col justify-content-center align-items-center">
        <div className="m-1 d-flex justify-content-center">
          {renderClues()}
        </div>
      </div>
    </div>
  );
}

export class MapGameInputProps {
  guesses: string[];
  correctnessArray: boolean[];
  guessBoxName: (index: number) => string;
  gaveUp: boolean;
  handleGuessInput: (e: React.FormEvent<HTMLInputElement>) => void;
  submitGuesses: (e: React.MouseEvent<HTMLInputElement>) => void;
  round: Round;
  clues?: string[];
}

export default MapGameInput;
