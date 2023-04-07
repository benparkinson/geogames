import { useQuery, useQueryClient } from "react-query";
import { GuessBox } from "../components/GuessBox";
import { useState } from "react";
import { Button } from "../components/Button";
import TripointMapWrapper from "../map/TripointMapWrapper";
import { serverEndpoint } from "../api/Gateway";
import RiverShapesMapWrapper from "../map/RiverShapesMapWrapper";

const RIVER_SHAPE_LOOKUP = "rivershapes";

function RiverShapes() {
  const { isLoading, error, data } = useQuery(RIVER_SHAPE_LOOKUP, () =>
    fetch(serverEndpoint() + "/api/river-shape").then((result) => result.json())
  );
  const [guesses, setGuesses] = useState(Array(1).fill(""));
  const [correctnessArray, setCorrectnessArray] = useState(Array(1).fill(null));
  const [gaveUp, setGaveUp] = useState(false);
  const [gameOver, setGameOver] = useState(false);
  const queryClient = useQueryClient();

  function canRender() {
    return !error && !isLoading;
  }

  function guessBoxName() {
    return "River name";
  }

  function renderBoxes() {
    return guesses.map((name, index) => (
      <GuessBox
        key={index}
        value={guesses[index]}
        name={guessBoxName()}
        onChange={handleGuessInput}
        correct={correctnessArray[index]}
        disabled={gaveUp}
      />
    ));
  }

  function handleGuessInput({ target }) {
    const newGuesses = guesses.map((guess, i) => {
      if (guessBoxName(i) === target.name && !correctnessArray[i]) {
        setCorrectnessByIndex(null, i);
        return target.value;
      }
      return guess;
    });
    setGuesses(newGuesses);
  }

  function setCorrectnessByIndex(correctness, index) {
    const arrayCopy = correctnessArray.slice();
    arrayCopy[index] = correctness;
    setCorrectnessArray(arrayCopy);
  }

  function renderMap(river) {
    return <RiverShapesMapWrapper river={river} gameOver={gameOver} gaveUp={gaveUp} />;
  }

  function submitGuesses() {
    const correctAnswers = new Set();
    correctAnswers.add(data.properties.name.toLowerCase());
    const newCorrectness = correctnessArray.slice();
    const guessesCopy = guesses.slice();

    guessesCopy.forEach((guess, index) => {
      if (correctAnswers.delete(guess.toLowerCase())) {
        newCorrectness[index] = true;
      } else {
        newCorrectness[index] = false;
      }
    });
    setCorrectnessArray(newCorrectness);
    setGameOver(newCorrectness.every((answer) => answer));
  }

  function giveUp() {
    const correctAnswers = [data.properties.name];
    const newGuesses = guesses.slice();
    correctAnswers.forEach((country, index) => {
      newGuesses[index] = country;
    });
    setGuesses(newGuesses);
    setGaveUp(true);
    setGameOver(true);
  }

  function newRivershape() {
    setGaveUp(false);
    setGameOver(false);
    setCorrectnessArray(Array(1).fill(null));
    setGuesses(Array(1).fill(""));
    queryClient.invalidateQueries(RIVER_SHAPE_LOOKUP);
  }

  return (
    <div className="d-flex flex-column vh-100">
      <div className="container flex-fill">
        <div id="map">{canRender() && renderMap(data)}</div>
        <div id="input" className="row align-items-center">
          <div id="text-boxes" className="col justify-content-center align-items-center">
            {canRender() && renderBoxes(data)}
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
            <Button bootstrapClass="btn-primary" text={"New River"} onClick={newRivershape} />
          </div>
        </div>
      </div>
    </div>
  );
}

export default RiverShapes;
