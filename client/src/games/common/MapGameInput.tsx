import { GuessBox } from "../../components/GuessBox";
import { Button } from "../../components/Button";

function MapGameInput({
  dataName,
  guesses,
  correctnessArray,
  guessBoxName,
  gaveUp,
  handleGuessInput,
  submitGuesses,
  giveUp,
  newData,
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

  return (
    <div id="input" className="row align-items-center">
      <div id="text-boxes" className="col justify-content-center align-items-center">
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

      <div id="new-game-div" className="col d-flex justify-content-center align-items-center">
        <Button bootstrapClass="btn-primary" text={"New " + dataName} onClick={newData} />
      </div>
    </div>
  );
}

export class MapGameInputProps {
  dataName: string;
  guesses: string[];
  correctnessArray: boolean[];
  guessBoxName: (index: number) => string;
  gaveUp: boolean;
  handleGuessInput: (e: React.FormEvent<HTMLInputElement>) => void;
  submitGuesses: (e: React.MouseEvent<HTMLInputElement>) => void;
  giveUp: (e: React.MouseEvent<HTMLInputElement>) => void;
  newData: (e: React.MouseEvent<HTMLInputElement>) => void;
}

export default MapGameInput;
