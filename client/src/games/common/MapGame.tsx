import { useState } from "react";
import { normaliseString } from "../../helper/stringHelper";
import MapGameInput from "./MapGameInput";
import { Round } from "./Model";

function MapGame<Type>({
  data,
  guessBoxCount,
  guessBoxName,
  MapComponent,
  correctAnswersFunction,
  checkAdditionalAnswers,
  round: round
}: MapGameProps<Type>): JSX.Element {
  const [guesses, setGuesses] = useState(Array(guessBoxCount).fill(""));
  const [correctnessArray, setCorrectnessArray] = useState(Array(guessBoxCount).fill(null));
  const [gaveUp, setGaveUp] = useState(false);
  const [gameOver, setGameOver] = useState(false);

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

  function setCorrectnessByIndex(correctness: boolean, index: number) {
    const arrayCopy = correctnessArray.slice();
    arrayCopy[index] = correctness;
    setCorrectnessArray(arrayCopy);
  }

  function renderMap() {
    return <MapComponent data={data} gameOver={gameOver} gaveUp={gaveUp} />;
  }

  function submitGuesses() {
    if (gameOver) {
      return;
    }

    const correctAnswers = new Set();
    const answersArray = correctAnswersFunction(data).map(answer => normaliseString(answer));
    answersArray.forEach(ans => correctAnswers.add(ans));

    const newCorrectness = correctnessArray.slice();
    const guessesCopy = guesses.slice();

    guessesCopy.forEach((guess, index) => {
      guess = checkAdditionalAnswers(guess, data);

      if (correctAnswers.delete(normaliseString(guess))) {
        newCorrectness[index] = true;
      } else {
        newCorrectness[index] = false;
      }
    });
    setCorrectnessArray(newCorrectness);
    setGameOver(newCorrectness.every(answer => answer));
  }

  function giveUp() {
    if (gameOver) {
      return;
    }

    const correctAnswers = correctAnswersFunction(data);
    const newGuesses = guesses.slice();
    correctAnswers.forEach((country, index) => {
      newGuesses[index] = country;
    });
    setGuesses(newGuesses);
    setGaveUp(true);
    setGameOver(true);
  }

  function clearInput() {
    setGaveUp(false);
    setGameOver(false);
    setCorrectnessArray(Array(guessBoxCount).fill(null));
    setGuesses(Array(guessBoxCount).fill(""));
  }

  if (round) {
    enrichRoundFunctions();
  }

  function enrichRoundFunctions() {
    const nextRound = round.nextRound;
    const prevRound = round.prevRound;

    round.nextRound = () => {
      nextRound();
      clearInput();
    }

    round.prevRound = () => {
      prevRound();
      clearInput();
    }
  }

  return (
    <div className="d-flex flex-column vh-100">
      <div className="container flex-fill">
        <div className="height-80-vh">{renderMap()}</div>
        <MapGameInput
          guesses={guesses}
          correctnessArray={correctnessArray}
          guessBoxName={guessBoxName}
          gaveUp={gaveUp}
          handleGuessInput={handleGuessInput}
          submitGuesses={submitGuesses}
          giveUp={giveUp}
          round={round}
        />
      </div>
    </div>
  );
}

export class MapGameProps<Type> {
  data: Type;
  guessBoxCount: number;
  guessBoxName: (index: number) => string;;
  MapComponent: any;
  correctAnswersFunction: (data: Type) => string[];
  checkAdditionalAnswers: (guess: string, data: Type) => string;
  round: Round;
}

export default MapGame;
