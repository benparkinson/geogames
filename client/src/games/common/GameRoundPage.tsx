import Head from "next/head";
import { useState } from "react";
import { useRound, useSubmitAnswer } from "../../api/games";
import { GameRoundView } from "../../api/types";
import GameSummary from "./GameSummary";
import { AnswerState, FINISHED, Round } from "./Model";
import LoadingSpinner from "../../components/LoadingSpinner";
import { findGameModule } from "../registry";

function GameRoundPage({ gameId }: { gameId: string }): React.JSX.Element {
    const [currentRound, setRound] = useState(0)
    const [showSummary, setShowSummary] = useState(false)
    const { error, data, isLoading } = useRound(gameId, currentRound);
    const submitAnswerMutation = useSubmitAnswer(gameId, currentRound);

    function nextRound(): void {
        setRound(currentRound + 1)
    }

    function goToRound(round: number): void {
        setRound(round)
        setShowSummary(false)
    }

    function submitAnswer(answerState: AnswerState): void {
        submitAnswerMutation.mutate(answerState);
    }

    function renderGame(data: GameRoundView) {
        const hasNextRound = currentRound < data.totalRoundCount - 1;
        const roundData = data.data;
        const gameCompleted = data.gameResult.gameState === FINISHED;
        const round: Round = {
            nextRound,
            goToRound,
            showSummary: () => setShowSummary(true),
            hasNextRound,
            currentRound,
            totalRoundCount: data.totalRoundCount,
            answerState: data.answerState,
            score: data.gameResult.score,
            gameCompleted,
            roundAnswerStates: data.gameResult.roundAnswerStates,
        };

        const gameModule = findGameModule(data.gameType.name);
        if (!gameModule) {
            return <div>Unknown game type...</div>
        }
        // The key resets the round's state, and the game type can change between rounds of a mixed game.
        return <gameModule.Component key={currentRound} data={roundData} round={round} answerCount={data.gameType.answerCount} submitAnswer={submitAnswer} />
    }

    function renderTitle() {
        if (!data) {
            return <title>Loading... | Geogames</title>
        }
        const gameModule = findGameModule(data.gameType.name);
        if (!gameModule) {
            return <title>Unknown game! | Geogames</title>
        }
        return <title>{`${gameModule.title} | Geogames`}</title>
    }

    return (
        <>
            <Head>
                {renderTitle()}
            </Head>
            {error ? <div className="p-4 text-danger" role="alert">Couldn&apos;t load this round. Please refresh.</div>
                : isLoading || !data ? <LoadingSpinner fullPage variant="success" />
                : showSummary ? <GameSummary gameId={gameId} totalRoundCount={data.totalRoundCount} onOpenRound={goToRound} />
                : renderGame(data)}
        </>
    );
}

export default GameRoundPage;
