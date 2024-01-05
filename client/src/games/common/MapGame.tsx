import { useQuery, useQueryClient } from "react-query";
import { useState } from "react";
import { serverEndpoint } from "../../api/gateway";
import { normaliseString } from "../../helper/stringHelper";
import MapGameInput from "./MapGameInput";
import Spinner from "../../components/Spinner";

function MapGame<Type>({
  dataName,
  serverRoute,
  guessBoxCount,
  guessBoxName,
  MapComponent,
  correctAnswersFunction,
  checkAdditionalAnswers,
  roundFunctions
}: MapGameProps<Type>): JSX.Element {
  const { error, data, isFetching } = useQuery(serverRoute, () =>
    fetch(serverEndpoint() + serverRoute).then(result => result.json())
  );
  const [guesses, setGuesses] = useState(Array(guessBoxCount).fill(""));
  const [correctnessArray, setCorrectnessArray] = useState(Array(guessBoxCount).fill(null));
  const [gaveUp, setGaveUp] = useState(false);
  const [gameOver, setGameOver] = useState(false);
  const queryClient = useQueryClient();

  function canRenderMap(): boolean {
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

  function setCorrectnessByIndex(correctness: boolean, index: number) {
    const arrayCopy = correctnessArray.slice();
    arrayCopy[index] = correctness;
    setCorrectnessArray(arrayCopy);
  }

  function renderMap() {
    return <MapComponent data={data} gameOver={gameOver} gaveUp={gaveUp} />;
  }

  function renderSpinner(): JSX.Element {
    return (
      <div className="d-flex height-80-vh justify-content-center align-items-center">
        <Spinner />
      </div>
    );
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

  function newData() {
    clearInput();
    queryClient.invalidateQueries(serverRoute);
  }

  function clearInput() {
    setGaveUp(false);
    setGameOver(false);
    setCorrectnessArray(Array(guessBoxCount).fill(null));
    setGuesses(Array(guessBoxCount).fill(""));
  }

  if (roundFunctions) {
    enrichRoundFunctions();
  }

  function enrichRoundFunctions() {
    const nextRound = roundFunctions.nextRound;
    const prevRound = roundFunctions.prevRound;

    roundFunctions.nextRound = () => {
      nextRound();
      clearInput();
    }

    roundFunctions.prevRound = () => {
      prevRound();
      clearInput();
    }
  }

  return (
    <div className="d-flex flex-column vh-100">
      <div className="container flex-fill">
        <div className="height-80-vh">{canRenderMap() ? renderMap() : renderSpinner()}</div>
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
          roundFunctions={roundFunctions}
        />
      </div>
    </div>
  );
}

export class MapGameProps<Type> {
  dataName: string;
  serverRoute: string;
  guessBoxCount: number;
  guessBoxName: (index: number) => string;;
  MapComponent: any;
  correctAnswersFunction: (data: Type) => string[];
  checkAdditionalAnswers: (guess: string, data: Type) => string;
  roundFunctions?: RoundFunctions;
}

export class RoundFunctions {
  nextRound: () => void;
  prevRound: () => void;
  hasPreviousRound: boolean;
  hasNextRound: boolean;
}

export default MapGame;
