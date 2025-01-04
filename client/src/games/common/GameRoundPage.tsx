import Head from "next/head";
import { useState } from "react";
import { useMutation, useQuery } from "react-query";
import { ReactQueryDevtools } from "react-query/devtools";
import { client, serverEndpoint } from "../../../src/api/gateway";
import Tripoint from "../Tripoint";
import { GameRoundModel, Round } from "../../../src/games/common/Model";
import Spinner from "../../../src/components/Spinner";
import RiverShapes from "../RiverShapes";

function GameRoundPage({ gameId }): JSX.Element {
    const [currentRound, setRound] = useState(0)
    const { error, data, isFetching } = useQuery(createUrl(), () =>
        fetch(serverEndpoint() + createUrl()).then(result => result.json())
    );
    const submitAnswerMutation = useMutation((answerState: string) =>
        fetch(serverEndpoint() + createUrl() + "/answers", {
            method: "POST", headers: {
                "Content-Type": "application/json",
            }, body: JSON.stringify({ answerState: answerState })
        }).then(result => result.json()),
        {
            onSuccess: () => {
                client.invalidateQueries(createUrl());
            }
        }
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

    function submitAnswer(answerState: string): void {
        submitAnswerMutation.mutate(answerState);
    }

    function renderGame(data: GameRoundModel) {
        const hasNextRound = currentRound < data.totalRoundCount - 1;
        const hasPrevRound = currentRound > 0;
        const roundData = JSON.parse(data.jsonBlob);
        const gameCompleted = data.gameResult.gameState == "FINISHED";
        const round = new Round(nextRound, prevRound, hasPrevRound, hasNextRound, currentRound, data.totalRoundCount, data.answerState,
            data.gameResult.score, gameCompleted);

        if (data.gameType.name == "TRIPOINT") {
            return <Tripoint tripoint={roundData} round={round} answerCount={data.gameType.answerCount} submitAnswer={submitAnswer} />
        } else if (data.gameType.name == "RIVERS_BY_SHAPE") {
            return <RiverShapes river={roundData} round={round} answerCount={data.gameType.answerCount} submitAnswer={submitAnswer} />
        }
        else {
            return <div>Unknown game type...</div>
        }
    }

    function renderTitle() {
        if (isFetching) {
            return <title>Loading... | Geogames</title>
        }
        if (data.gameType.name == "TRIPOINT") {
            return <title>Tripoint | Geogames</title>
        } else if (data.gameType.name == "RIVERS_BY_SHAPE") {
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

export const CORRECT_ANSWER: string = "CORRECT";
export const GAVE_UP: string = "GAVE_UP";
export const UNANSWERED: string = "UNANSWERED";