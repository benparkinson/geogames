import { useState } from "react";
import { normaliseString } from "../../helper/stringHelper";
import MapGameInput from "./MapGameInput";
import { Round } from "./Model";
import { CORRECT_ANSWER, GAVE_UP, UNANSWERED } from "./GameRoundPage";
import { Button, Modal, Stack } from "react-bootstrap";
import HelpButton from "./HelpButton";
import MapGameScore from "./MapGameScore";

function MapGame<Type>({
  data,
  answerCount,
  MapComponent,
  correctAnswersFunction,
  round,
  explanation,
  clues,
  displayMapHint,
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
    if (correctCount === answerCount) {
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

  function setCorrectAnswers(_userCorrect: boolean) {
    const correctAnswers = correctAnswersFunction(data);

    setAttemptedGuesses(correctAnswers.map((answer, _index) => {
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

  function renderCorrectAnswerChips() {
    return <div className="p-1 m-1 row">
      <Stack direction="horizontal" gap={1} className="mx-auto">
        {[...Array(answerCount)].map((_value: undefined, index: number) => <div key={index} className="mx-auto d-flex justify-content-center">
          <Button disabled variant={determineAnswerChipColour(index)} />
        </div>
        )}
      </Stack>
    </div>;
  }

  function determineAnswerChipColour(chipIndex: number) {
    const correctCount = attemptedGuesses.filter((answer: AnswerModel) => {
      return answer.correct;
    }).length;

    if (correctCount - 1 < chipIndex) {
      return "secondary";
    }

    return gaveUp ? "danger" : "success";
  }

  return (
    <div className="d-flex flex-column background">
      <div className="container flex-fill map-game">

        <Stack direction="horizontal" gap={3}>
          <div className="pt-1">
            <MapGameScore round={round} />
          </div>
          <div className="ms-auto pt-1">
            <HelpButton giveUp={giveUp} openExplanationModal={openGameExplanationModal} openClueModal={clues && openRoundClueModal} />
          </div>
        </Stack>

        <div className="map">{renderMap()}</div>

        {answerCount > 1 && renderCorrectAnswerChips()}

        <MapGameInput
          gameOver={gameOver}
          round={round}
          answers={attemptedGuesses}
          submitGuess={submitGuess}
          displayMapHint={displayMapHint}
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
  answerCount: number;
  MapComponent: any;
  correctAnswersFunction: (data: Type) => string[];
  round: Round;
  explanation: string;
  clues?: string[];
  submitAnswer: (answerState: string) => void;
  answerState: string;
  isAnswerCorrect: (guess: string, data: Type) => boolean;
  displayMapHint?: () => void;
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
