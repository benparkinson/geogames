import { useQuery, useQueryClient } from "react-query";
import { GuessBox } from "../components/GuessBox";
import { useState } from "react";
import { Button } from "../components/Button";
import TripointMapWrapper from "../map/TripointMapWrapper";
import { serverEndpoint } from "../api/Gateway";

const TRIPOINT_LOOKUP = "tripoints";

function Tripoint() {
  const { isLoading, error, data } = useQuery(TRIPOINT_LOOKUP, () =>
    fetch(serverEndpoint() + "/api/tripoint").then((result) => result.json())
  );
  const [guesses, setGuesses] = useState(Array(3).fill(""));
  const [correctnessArray, setCorrectnessArray] = useState(Array(3).fill(null));
  const [gaveUp, setGaveUp] = useState(false);
  const [gameOver, setGameOver] = useState(false);
  const queryClient = useQueryClient();

  function canRender() {
    return !error && !isLoading;
  }

  function guessBoxName(index) {
    return "Country " + (index + 1);
  }

  function countryNames(tripoint) {
    return tripoint.countries.map((country) => country.properties.name);
  }

  function extractCountries(tripoint) {
    return tripoint.countries.map((country) => country.properties);
  }

  function renderBoxes(tripoint) {
    return countryNames(tripoint).map((name, index) => (
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

  function renderMap(tripoint) {
    return <TripointMapWrapper tripoint={tripoint} gameOver={gameOver} gaveUp={gaveUp} />;
  }

  function submitGuesses() {
    const countries = extractCountries(data);

    const correctAnswers = new Set(
      countryNames(data)
        .slice()
        .map((name) => name.toLowerCase())
    );
    const newCorrectness = correctnessArray.slice();
    const guessesCopy = guesses.slice();

    guessesCopy.forEach((guess, index) => {
      countries.forEach((country) =>
        country.additionalNames.forEach((additionalName) => {
          if (guess.toLowerCase() === additionalName.toLowerCase()) {
            guess = country.name;
          }
        })
      );

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
    const correctAnswers = countryNames(data).slice();
    const newGuesses = guesses.slice();
    correctAnswers.forEach((country, index) => {
      newGuesses[index] = country;
    });
    setGuesses(newGuesses);
    setGaveUp(true);
    setGameOver(true);
  }

  function newTripoint() {
    setGaveUp(false);
    setGameOver(false);
    setCorrectnessArray(Array(3).fill(null));
    setGuesses(Array(3).fill(""));
    queryClient.invalidateQueries(TRIPOINT_LOOKUP);
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
            <Button bootstrapClass="btn-primary" text={"New Tripoint"} onClick={newTripoint} />
          </div>
        </div>
      </div>
    </div>
  );
}

export default Tripoint;
