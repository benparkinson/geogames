import { useQuery, useQueryClient } from "react-query";
import { useState, useEffect } from "react";
import { serverEndpoint } from "../../api/gateway";
import { normaliseString } from "../../helper/stringHelper";
import MapGameInput from "./MapGameInput";
import Spinner from "../../components/Spinner";

function MapGame<Type>({
  dataName,
  serverRoute,
  queryParamsFromPreviousResponse,
  guessBoxCount,
  guessBoxName,
  MapComponent,
  correctAnswersFunction,
  checkAdditionalAnswers
}: MapGameProps<Type>): JSX.Element {
  const [previousResponse, setPreviousResponse] = useState(null);
  const { error, data, isFetching } = useQuery(serverRoute, getData()
  );
  const [guesses, setGuesses] = useState(Array(guessBoxCount).fill(""));
  const [correctnessArray, setCorrectnessArray] = useState(Array(guessBoxCount).fill(null));
  const [gaveUp, setGaveUp] = useState(false);
  const [gameOver, setGameOver] = useState(false);

  const queryClient = useQueryClient();

  useEffect(() => {
    if (data) {
      setPreviousResponse(data)
    }
  }, [data])

  function getData() {
    const endpoint = serverEndpoint() + serverRoute;
    const params = queryParamsFromPreviousResponse(previousResponse);

    return () => fetch(endpoint + "?" + params).then(result => result.json());
  }

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

    console.log(correctAnswers);

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
    setGaveUp(false);
    setGameOver(false);
    setCorrectnessArray(Array(guessBoxCount).fill(null));
    setGuesses(Array(guessBoxCount).fill(""));
    queryClient.invalidateQueries(serverRoute);
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
        />
      </div>
    </div>
  );
}

export class MapGameProps<Type> {
  dataName: string;
  serverRoute: string;
  queryParamsFromPreviousResponse: (data: Type) => string;
  guessBoxCount: number;
  guessBoxName: (index: number) => string;;
  MapComponent: any;
  correctAnswersFunction: (data: Type) => string[];
  checkAdditionalAnswers: (guess: string, data: Type) => string;
}

export default MapGame;
