import Head from "next/head";
import { useState } from "react";
import { useQuery } from "react-query";
import { ReactQueryDevtools } from "react-query/devtools";
import { serverEndpoint } from "../../../src/api/gateway";
import Tripoint from "../Tripoint";
import { GameRoundModel, Round } from "../../../src/games/common/Model";
import Spinner from "../../../src/components/Spinner";
import RiverShapes from "../RiverShapes";

function GameRoundPage({ gameId }): JSX.Element {
    const [currentRound, setRound] = useState(0)
    const { error, data, isFetching } = useQuery(createUrl(), () =>
        fetch(serverEndpoint() + createUrl()).then(result => result.json())
    );

    function createUrl() {
        return "/api/games/" + gameId + "/rounds/" + currentRound;
    }

    function nextRound(): void {
        setRound(currentRound + 1)
    }

    function prevRound(): void {
        setRound(currentRound - 1)
    }

    function renderGame(data: GameRoundModel) {
        const hasNextRound = currentRound < data.totalRoundCount - 1;
        const hasPrevRound = currentRound > 0;
        const roundData = JSON.parse(data.jsonBlob);
        const round = new Round(nextRound, prevRound, hasPrevRound, hasNextRound, currentRound, data.totalRoundCount);
        if (data.gameType == "TRIPOINT") {
            return <Tripoint tripoint={roundData} round={round} />
        } else if (data.gameType == "RIVERS_BY_SHAPE") {
            return <RiverShapes river={roundData} round={round} />
        }
        else {
            return <div>Unknown game type...</div>
        }
    }

    function renderTitle() {
        if (isFetching) {
            return <title>Loading... | Geogames</title>
        }
        if (data.gameType == "TRIPOINT") {
            return <title>Tripoint | Geogames</title>
        } else if (data.gameType == "RIVERS_BY_SHAPE") {
            return <title>Rivers | Geogames</title>
        }
        else {
            return <title>Unknown game! | Geogames</title>
        }
    }

    function renderSpinner(): JSX.Element {
        return (
            <div className="d-flex height-80-vh justify-content-center align-items-center">
                <Spinner />
            </div>
        );
    }

    return (
        <>
            <Head>
                {renderTitle()}
            </Head>
            {isFetching ? renderSpinner() : renderGame(data)}
            <ReactQueryDevtools initialIsOpen={false} />
        </>
    );
}

export default GameRoundPage;
