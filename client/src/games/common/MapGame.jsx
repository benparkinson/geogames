import { useQuery, useQueryClient } from "react-query";
import { useState } from "react";
import { serverEndpoint } from "../../api/Gateway";
import MapGameInput from "./MapGameInput";
import Spinner from "../../components/Spinner";

function MapGame({
  dataName,
  serverRoute,
  guessBoxCount,
  guessBoxName,
  MapComponent,
  correctAnswersFunction,
  checkAdditionalAnswers,
}) {
  const { error, data, isFetching } = useQuery(serverRoute, () =>
    fetch(serverEndpoint() + serverRoute).then((result) => result.json())
  );
  const [guesses, setGuesses] = useState(Array(guessBoxCount).fill(""));
  const [correctnessArray, setCorrectnessArray] = useState(Array(guessBoxCount).fill(null));
  const [gaveUp, setGaveUp] = useState(false);
  const [gameOver, setGameOver] = useState(false);
  const queryClient = useQueryClient();

  function canRenderMap() {
    return !error && !isFetching;
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

  function renderMap() {
    return <MapComponent data={data} gameOver={gameOver} gaveUp={gaveUp} />;
  }

  function renderSpinner() {
    return (
      <div className="d-flex height-80-vh justify-content-center align-items-center">
        <Spinner />
      </div>
    );
  }

  function submitGuesses() {
    const correctAnswers = new Set();
    const answersArray = correctAnswersFunction(data).map((answer) => answer.toLowerCase());
    answersArray.forEach((ans) => correctAnswers.add(ans));

    const newCorrectness = correctnessArray.slice();
    const guessesCopy = guesses.slice();

    guessesCopy.forEach((guess, index) => {
      guess = checkAdditionalAnswers(guess, data);

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
    const correctAnswers = correctAnswersFunction(data);
    const newGuesses = guesses.slice();
    correctAnswers.forEach((country, index) => {
      newGuesses[index] = country;
    });
    setGuesses(newGuesses);
    setGaveUp(true);
    setGameOver(true);
  }

  function newData() {
    setGaveUp(false);
    setGameOver(false);
    setCorrectnessArray(Array(guessBoxCount).fill(null));
    setGuesses(Array(guessBoxCount).fill(""));
    queryClient.invalidateQueries(serverRoute);
  }

  return (
    <div className="d-flex flex-column vh-100">
      <div className="container flex-fill">
        <div className="height-80-vh">{canRenderMap() ? renderMap(data) : renderSpinner()}</div>
        <MapGameInput
          dataName={dataName}
          guesses={guesses}
          correctnessArray={correctnessArray}
          guessBoxName={guessBoxName}
          gaveUp={gaveUp}
          handleGuessInput={handleGuessInput}
          submitGuesses={submitGuesses}
          giveUp={giveUp}
          newData={newData}
        />
      </div>
    </div>
  );
}

export default MapGame;
