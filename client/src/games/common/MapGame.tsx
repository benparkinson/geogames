import { useRef, useState } from "react";
import { normaliseString } from "../../helper/stringHelper";
import MapGameInput from "./MapGameInput";
import { Round } from "./Model";
import { CORRECT_ANSWER, GAVE_UP, UNANSWERED } from "./GameRoundPage";
import OverlayTrigger from 'react-bootstrap/OverlayTrigger';
import Popover from 'react-bootstrap/Popover';
import { Button, Modal, Overlay, Tooltip } from "react-bootstrap";
import HelpButton from "./HelpButton";

function MapGame<Type>({
  data,
  guessBoxCount,
  guessBoxName,
  MapComponent,
  correctAnswersFunction,
  checkAdditionalAnswers,
  round,
  explanation,
  clues,
  submitAnswer,
  answerState
}: MapGameProps<Type>): JSX.Element {
  const [guesses, setGuesses] = useState(Array(guessBoxCount).fill(""));
  const [correctnessArray, setCorrectnessArray] = useState(Array(guessBoxCount).fill(null));
  const [gaveUp, setGaveUp] = useState(answerState === GAVE_UP);
  const [gameOver, setGameOver] = useState(answerState !== UNANSWERED);
  const [openExplanationModal, setOpenExplanationModal] = useState(false);
  const [showHelp, setShowHelp] = useState(false);
  const target = useRef(null);

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
    const correct = newCorrectness.every(answer => answer)
    if (correct) {
      submitAnswer(CORRECT_ANSWER);
    }
    setGameOver(correct);
  }

  function giveUp() {
    if (gameOver) {
      return;
    }

    setCorrectAnswers(false);
    submitAnswer(GAVE_UP);
    setGaveUp(true);
    setGameOver(true);
  }

  function setCorrectAnswers(userCorrect: boolean) {
    const correctAnswers = correctAnswersFunction(data);
    const newGuesses = guesses.slice();
    correctAnswers.forEach((country, index) => {
      newGuesses[index] = country;
    });
    setGuesses(newGuesses);
    const newCorrectness = correctnessArray.slice();
    newCorrectness.fill(userCorrect);
    setCorrectnessArray(newCorrectness);
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

  if (gameOver && guesses.every(guess => guess === "")) {
    setCorrectAnswers(!gaveUp);
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

  function handleHelpButtonClick() {

  }

  function openGameExplanationModal() {
    setOpenExplanationModal(true);
  }

  return (
    <div className="d-flex flex-column">
      <div className="container flex-fill map-game">
        <HelpButton giveUp={giveUp} openExplanationModal={openGameExplanationModal} />
        <div className="map">{renderMap()}</div>
        <MapGameInput
          guesses={guesses}
          correctnessArray={correctnessArray}
          guessBoxName={guessBoxName}
          gaveUp={gaveUp}
          handleGuessInput={handleGuessInput}
          submitGuesses={submitGuesses}
          round={round}
          clues={clues}
        />
      </div>
      <Modal show={openExplanationModal} onHide={() => setOpenExplanationModal(false)}>
        <Modal.Header closeButton>
          <Modal.Title>
            Help
          </Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {explanation}
        </Modal.Body>
        <Modal.Footer>
          <Button variant={"primary"} onClick={() => setOpenExplanationModal(false)}>Ok!</Button>
        </Modal.Footer>
      </Modal>
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
  explanation: string;
  clues?: string[];
  submitAnswer: (answerState: string) => void;
  answerState: string;
}

export default MapGame;
