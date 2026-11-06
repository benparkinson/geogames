import { useCreateGame } from "../api/games";
import { useState } from "react";
import Dropdown from 'react-bootstrap/Dropdown';
import DropdownButton from 'react-bootstrap/DropdownButton';
import Modal from 'react-bootstrap/Modal';
import { Button, Spinner } from "react-bootstrap";

function HomePage(): JSX.Element {
    const { createGame, isLoading } = useCreateGame();
    const [openModal, setOpenModal] = useState(false);
    const [selectedGameType, setSelectedGameType] = useState(TripointGameType);
    const [numberOfRounds, setNumberOfRounds] = useState(3);

    function startGame(): void {
        createGame(selectedGameType.name, numberOfRounds);
    }

    function renderGameTypeDropDownItem(gameType: GameType): JSX.Element {
        return <Dropdown.Item onClick={() => setSelectedGameType(gameType)}>{gameType.displayName}</Dropdown.Item>
    }

    function renderRoundCountDropDownItem(roundCount: number): JSX.Element {
        return <Dropdown.Item onClick={() => setNumberOfRounds(roundCount)}>{roundCount}</Dropdown.Item>
    }

    return (
        <div className="d-flex vh-100" id="main-page">
            <div className="container d-flex justify-content-center align-items-center text-center">
                <div className="row d-flex flex-column">
                    <h4>Welcome to Geogames (working title)</h4>
                    <div className="m-1">
                        <Button variant="info" onClick={() => setOpenModal(true)}>New game</Button>
                    </div>
                </div>
                <Modal show={openModal} onHide={() => setOpenModal(false)}>
                    <Modal.Header closeButton>
                        <Modal.Title>
                            New game?
                        </Modal.Title>
                    </Modal.Header>
                    <Modal.Body>
                        <div className="d-flex">
                            <div className="m-2">Game type: </div>
                            <DropdownButton id="dropdown-basic-button" title={selectedGameType.displayName}>
                                {renderGameTypeDropDownItem(TripointGameType)}
                                {renderGameTypeDropDownItem(RiversGameType)}
                                {renderGameTypeDropDownItem(RandomSelectionGameType)}
                            </DropdownButton>
                        </div>
                        <div className="d-flex">
                            <div className="m-2">Number of rounds: </div>
                            <DropdownButton id="dropdown-basic-button" title={numberOfRounds}>
                                {renderRoundCountDropDownItem(1)}
                                {renderRoundCountDropDownItem(2)}
                                {renderRoundCountDropDownItem(3)}
                                {renderRoundCountDropDownItem(4)}
                                {renderRoundCountDropDownItem(5)}
                            </DropdownButton>
                        </div>
                    </Modal.Body>
                    <Modal.Footer>
                        <Button variant="secondary" onClick={() => setOpenModal(false)}>Close</Button>
                        <Button variant="info" onClick={() => startGame()} disabled={isLoading}>{isLoading ? <Spinner /> : "Go!"}</Button>
                    </Modal.Footer>
                </Modal>
            </div>
        </div >
    );
}

class GameType {
    name: string;
    displayName: string;
    constructor(name: string, displayName: string) {
        this.name = name;
        this.displayName = displayName;
    }
}

const TripointGameType = new GameType("TRIPOINT", "Tripoint");
const RiversGameType = new GameType("RIVERS_BY_SHAPE", "Rivers by shape");
const RandomSelectionGameType = new GameType("RANDOM", "Random selection");

export default HomePage;
