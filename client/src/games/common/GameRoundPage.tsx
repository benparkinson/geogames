import Head from "next/head";
import { useState } from "react";
import { useQuery } from "react-query";
import { ReactQueryDevtools } from "react-query/devtools";
import { serverEndpoint } from "../../../src/api/gateway";
import Tripoint from "../../../src/games/TripointRound";
import { GameRoundModel } from "../../../src/games/common/Model";
import Spinner from "../../../src/components/Spinner";

function GameRoundPage({ gameId }): JSX.Element {
    const [round, setRound] = useState(0)
    const { error, data, isFetching } = useQuery(createUrl(), () =>
        fetch(serverEndpoint() + createUrl()).then(result => result.json())
    );

    function createUrl() {
        return "/api/games/" + gameId + "/rounds/" + round;
    }

    function nextRound(): void {
        setRound(round + 1)
    }

    function prevRound(): void {
        setRound(round - 1)
    }

    function renderGame(data: GameRoundModel) {
        const hasNextRound = round < data.totalRoundCount - 1;
        const hasPrevRound = round > 0;
        if (data.gameType == "TRIPOINT") {
            const tripoint = JSON.parse(data.jsonBlob);
            return <Tripoint tripoint={tripoint} nextRound={nextRound} prevRound={prevRound} hasNextRound={hasNextRound} hasPrevRound={hasPrevRound} />
        } else if (data.gameType == "RIVERS_BY_SHAPE") {
            return <div>Rivers</div>
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
