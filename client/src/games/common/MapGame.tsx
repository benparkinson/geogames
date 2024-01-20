import { useState } from "react";
import { normaliseString } from "../../helper/stringHelper";
import MapGameInput from "./MapGameInput";
import { Round } from "./Model";
import { CORRECT_ANSWER, GAVE_UP, UNANSWERED } from "./GameRoundPage";
import { Button, Modal } from "react-bootstrap";
import HelpButton from "./HelpButton";

function MapGame<Type>({
  data,
  guessBoxCount,
  MapComponent,
  correctAnswersFunction,
  round,
  explanation,
  clues,
  submitAnswer,
  answerState,
  isAnswerCorrect
}: MapGameProps<Type>): JSX.Element {
  const [attemptedGuesses, setAttemptedGuesses] = useState(new Array());
  const [gaveUp, setGaveUp] = useState(answerState === GAVE_UP);
  const [gameOver, setGameOver] = useState(answerState !== UNANSWERED);
  const [openExplanationModal, setOpenExplanationModal] = useState(false);
  const [openClueModal, setOpenClueModal] = useState(false);

  function renderMap() {
    return <MapComponent data={data} gameOver={gameOver} gaveUp={gaveUp} />;
  }

  function submitGuess(guess: string) {
    if (gameOver) {
      return false;
    }

    if (guess === "") {
      return false;
    }

    const allGuesses = attemptedGuesses.map((answer: AnswerModel) => {
      return normaliseString(answer.answer)
    });

    if (allGuesses.includes(normaliseString(guess))) {
      return false;
    }

    const correct = isAnswerCorrect(guess, data)
    const answer = new AnswerModel(guess, correct);

    const newAttemptedGuesses = [...attemptedGuesses, answer];

    const correctCount = newAttemptedGuesses.filter((answer: AnswerModel) => {
      return answer.correct;
    }).length;

    setAttemptedGuesses(newAttemptedGuesses);
    if (correctCount === guessBoxCount) {
      console.log("All correct!")
      submitAnswer(CORRECT_ANSWER);
      setGameOver(true);
    }
    return true;
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

    setAttemptedGuesses(correctAnswers.map((answer, index) => {
      return new AnswerModel(answer, true);
    }));
  }

  function clearInput() {
    setGaveUp(false);
    setGameOver(false);
  }

  if (round) {
    enrichRoundFunctions();
  }

  if (gameOver && attemptedGuesses.length === 0) {
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

  function openGameExplanationModal() {
    setOpenExplanationModal(true);
  }

  function openRoundClueModal() {
    setOpenClueModal(true);
  }

  return (
    <div className="d-flex flex-column">
      <div className="container flex-fill map-game">
        <HelpButton giveUp={giveUp} openExplanationModal={openGameExplanationModal} openClueModal={openRoundClueModal} />
        <div className="map">{renderMap()}</div>
        <MapGameInput
          gameOver={gameOver}
          round={round}
          clues={clues}
          answers={attemptedGuesses}
          submitGuess={submitGuess}
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
      <Modal show={openClueModal} onHide={() => setOpenClueModal(false)}>
        <Modal.Header closeButton>
          <Modal.Title>
            Clues
          </Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {clues && clues.map((clue, index) => (
            <div key={index} className="m-1">
              {clue}
            </div>
          ))}
        </Modal.Body>
        <Modal.Footer>
          <Button variant={"primary"} onClick={() => setOpenClueModal(false)}>Thanks!</Button>
        </Modal.Footer>
      </Modal>
    </div>
  );
}

export class MapGameProps<Type> {
  data: Type;
  guessBoxCount: number;
  MapComponent: any;
  correctAnswersFunction: (data: Type) => string[];
  round: Round;
  explanation: string;
  clues?: string[];
  submitAnswer: (answerState: string) => void;
  answerState: string;
  isAnswerCorrect: (guess: string, data: Type) => boolean;
}

export class AnswerModel {
  answer: string;
  correct: boolean;

  constructor(answer: string, correct: boolean) {
    this.answer = answer;
    this.correct = correct;
  }
}

export default MapGame;
