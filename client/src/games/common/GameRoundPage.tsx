import Head from "next/head";
import { useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
import { client, serverEndpoint } from "../../../src/api/gateway";
import { fetchRound, roundUrl } from "../../../src/api/games";
import GameSummary from "./GameSummary";
import Tripoint from "../Tripoint";
import { GameRoundModel, Round, UNANSWERED } from "../../../src/games/common/Model";
import { Spinner } from "react-bootstrap";
import RiverShapes from "../RiverShapes";

function GameRoundPage({ gameId }: { gameId: string }): React.JSX.Element {
    const [currentRound, setRound] = useState(0)
    const [showSummary, setShowSummary] = useState(false)
    const { error, data, isLoading } = useQuery({
        queryKey: [createUrl()],
        queryFn: () => fetchRound(gameId, currentRound),
    });
    const submitAnswerMutation = useMutation({
        mutationFn: (answerState: string) =>
            fetch(serverEndpoint() + createUrl() + "/answers", {
                method: "POST", headers: {
                    "Content-Type": "application/json",
                }, body: JSON.stringify({ answerState: answerState })
            }).then(result => result.json()),
        onSuccess: () => {
            client.invalidateQueries({ queryKey: [createUrl()] });
        },
    });

    function createUrl() {
        return roundUrl(gameId, currentRound);
    }

    function nextRound(): void {
        setRound(currentRound + 1)
    }

    function goToRound(round: number): void {
        setRound(round)
        setShowSummary(false)
    }

    function submitAnswer(answerState: string): void {
        submitAnswerMutation.mutate(answerState);
    }

    function renderGame(data: GameRoundModel) {
        const hasNextRound = currentRound < data.totalRoundCount - 1;
        const roundData = JSON.parse(data.jsonBlob);
        const gameCompleted = data.gameResult.gameState == "FINISHED";
        const round = new Round({
            nextRound,
            goToRound,
            showSummary: () => setShowSummary(true),
            hasNextRound,
            currentRound,
            totalRoundCount: data.totalRoundCount,
            answerState: data.answerState,
            score: data.gameResult.score,
            gameCompleted,
            roundAnswerStates: roundAnswerStates(data),
        });

        if (data.gameType.name == "TRIPOINT") {
            return <Tripoint key={currentRound} tripoint={roundData} round={round} answerCount={data.gameType.answerCount} submitAnswer={submitAnswer} />
        } else if (data.gameType.name == "RIVERS_BY_SHAPE") {
            return <RiverShapes key={currentRound} river={roundData} round={round} answerCount={data.gameType.answerCount} submitAnswer={submitAnswer} />
        }
        else {
            return <div>Unknown game type...</div>
        }
    }

    // Older backends don't send roundAnswerStates; fall back to what this response tells us.
    function roundAnswerStates(data: GameRoundModel): string[] {
        if (Array.isArray(data.gameResult?.roundAnswerStates)) {
            return data.gameResult.roundAnswerStates;
        }
        const states: string[] = Array(data.totalRoundCount).fill(UNANSWERED);
        states[currentRound] = data.answerState;
        return states;
    }

    function renderTitle() {
        if (!data) {
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

    function renderSpinner(): React.JSX.Element {
        return (
            <div className="d-flex min-vh-100 justify-content-center align-items-center">
                <Spinner animation="border" variant="success" />
            </div>
        );
    }

    return (
        <>
            <Head>
                {renderTitle()}
            </Head>
            {error ? <div className="p-4 text-danger" role="alert">Couldn&apos;t load this round. Please refresh.</div>
                : isLoading || !data ? renderSpinner()
                : showSummary ? <GameSummary gameId={gameId} totalRoundCount={data.totalRoundCount} onOpenRound={goToRound} />
                : renderGame(data)}
            <ReactQueryDevtools initialIsOpen={false} />
        </>
    );
}

export default GameRoundPage;
