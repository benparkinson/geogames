import { GuessBox } from "../../components/GuessBox";
import { Button } from "../../components/Button";
import { Round } from "./Model";

function MapGameInput({
  guesses,
  correctnessArray,
  guessBoxName,
  gaveUp,
  handleGuessInput,
  submitGuesses,
  giveUp,
  round: round,
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

  return (
    <div id="input" className="row align-items-center">
      <div className="col justify-content-center align-items-center">
        {renderBoxes()}
      </div>

      <div className="col justify-content-center align-items-center">
        <div className="m-1 d-flex justify-content-center">
          <Button bootstrapClass="btn-success" text={"Submit"} onClick={submitGuesses} />
        </div>
        <div className="m-1 d-flex justify-content-center">
          <Button bootstrapClass="btn-secondary" text={"Give Up"} onClick={giveUp} />
        </div>
      </div>

      <div id="new-game-div" className="col justify-content-center align-items-center">
        <div className="m-1 d-flex justify-content-center">
          Round {round.currentRound + 1}/{round.totalRoundCount}
        </div>
        {renderRoundButtons()}
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
  giveUp: (e: React.MouseEvent<HTMLInputElement>) => void;
  round: Round;
}

export default MapGameInput;
