import { useMutation } from "react-query";
import { serverEndpoint } from "../api/gateway";
import { Button } from "./Button";
import { useRouter } from "next/router";
import { useEffect, useState } from "react";
import Dropdown from 'react-bootstrap/Dropdown';
import DropdownButton from 'react-bootstrap/DropdownButton';
import Modal from 'react-bootstrap/Modal';

function HomePage(): JSX.Element {
    const router = useRouter();
    const newGameMutation = useMutation((gameType: string) =>
        fetch(serverEndpoint() + "/api/games?gameType=" + gameType, { method: "POST" }).then(result => result.json()),
        {
            onSuccess: (data: { id: any; }) => {
                const gameId = data.id;
                router.push("/games/" + gameId)
            }
        }
    );
    const [openModal, setOpenModal] = useState(false)
    const [selectedGameType, setSelectedGameType] = useState(TripointGameType)
    //useEffect(() => { ReactModal.setAppElement('#main-page'); }, []);

    function startGame(): void {
        newGameMutation.mutate(selectedGameType.name);
    }

    return (
        <div className="d-flex vh-100" id="main-page">
            <div className="container d-flex justify-content-center align-items-center text-center">
                <div className="row d-flex flex-column">
                    <h4>Welcome to Geogames (working title)</h4>
                    <div className="m-1">
                        <Button text={"New game (5 rounds)"} bootstrapClass={"btn-info"} onClick={() => setOpenModal(true)}></Button>
                    </div>
                </div>
                <Modal show={openModal} onHide={() => setOpenModal(false)}>
                    <Modal.Header closeButton>
                        <Modal.Title>
                            New game?
                        </Modal.Title>
                    </Modal.Header>
                    <Modal.Body className="d-flex">
                        <div className="m-2">Game type: </div>
                        <DropdownButton id="dropdown-basic-button" title={selectedGameType.displayName}>
                            <Dropdown.Item onClick={() => setSelectedGameType(TripointGameType)}>{TripointGameType.displayName}</Dropdown.Item>
                            <Dropdown.Item onClick={() => setSelectedGameType(RiversGameType)}>{RiversGameType.displayName}</Dropdown.Item>
                        </DropdownButton>
                    </Modal.Body>
                    <Modal.Footer>
                        <Button text={"Close"} bootstrapClass={"btn-secondary"} onClick={() => setOpenModal(false)}></Button>
                        <Button text={"Go!"} bootstrapClass={"btn-info"} onClick={() => startGame()}></Button>
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

export default HomePage;
