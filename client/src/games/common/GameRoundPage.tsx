import Head from "next/head";
import { useEffect, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { roundQuery, useRound, useSubmitAnswer } from "../../api/games";
import { GameRoundView } from "../../api/types";
import GameSummary from "./GameSummary";
import { FINISHED, Round, RoundAnswer, UNANSWERED } from "./Model";
import LoadingSpinner from "../../components/LoadingSpinner";
import { findGameModule } from "../registry";

function GameRoundPage({ gameId }: { gameId: string }): React.JSX.Element {
    // Until the player acts, resume at the first unanswered round, which the first round's game result tells us.
    const [chosenRound, setRound] = useState<number | null>(null)
    const [summaryChoice, setSummaryChoice] = useState<boolean | null>(null)
    const firstRound = useRound(gameId, 0).data;
    // Until the player acts, a finished game opens on its results rather than on the first round.
    const showSummary = summaryChoice ?? (chosenRound === null && firstRound?.gameResult.gameState === FINISHED);
    const resumeRound = Math.max(0, firstRound?.gameResult.roundAnswerStates.indexOf(UNANSWERED) ?? 0);
    const currentRound = chosenRound ?? resumeRound;
    const { error, data, isLoading } = useRound(gameId, currentRound);
    const submitAnswerMutation = useSubmitAnswer(gameId, currentRound, data?.totalRoundCount);
    const queryClient = useQueryClient();

    // A round answered before this page loaded: its next round is the likely next click. Does nothing when cached.
    const answered = data !== undefined && data.answerState !== UNANSWERED;
    const totalRoundCount = data?.totalRoundCount ?? 0;
    useEffect(() => {
        if (answered && currentRound + 1 < totalRoundCount) {
            void queryClient.prefetchQuery(roundQuery(gameId, currentRound + 1));
        }
    }, [answered, currentRound, totalRoundCount, gameId, queryClient]);

    function nextRound(): void {
        setRound(currentRound + 1)
    }

    function goToRound(round: number): void {
        setRound(round)
        setSummaryChoice(false)
    }

    function submitAnswer(answer: RoundAnswer): void {
        // Pin the round, or the refreshed game result would move the resume point on by itself.
        setRound(currentRound)
        submitAnswerMutation.mutate(answer);
    }

    function renderGame(data: GameRoundView) {
        const hasNextRound = currentRound < data.totalRoundCount - 1;
        const roundData = data.data;
        const gameCompleted = data.gameResult.gameState === FINISHED;
        const round: Round = {
            nextRound,
            goToRound,
            showSummary: () => setSummaryChoice(true),
            hasNextRound,
            currentRound,
            totalRoundCount: data.totalRoundCount,
            answerState: data.answerState,
            hintCount: data.gameType.hintCount,
            guessLimit: data.gameType.guessLimit,
            hintsUsed: data.hintsUsed,
            points: data.points,
            score: data.gameResult.score,
            maxScore: data.gameResult.maxScore,
            perfectRounds: data.gameResult.perfectRounds,
            gameCompleted,
            roundAnswerStates: data.gameResult.roundAnswerStates,
            roundPoints: data.gameResult.roundPoints,
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
            return <title>Loading... | GeoGauntlet</title>
        }
        const gameModule = findGameModule(data.gameType.name);
        if (!gameModule) {
            return <title>Unknown game! | GeoGauntlet</title>
        }
        return <title>{`${gameModule.title} | GeoGauntlet`}</title>
    }

    return (
        <>
            <Head>
                {renderTitle()}
            </Head>
            {error ? <div className="p-4 text-danger" role="alert">Couldn&apos;t load this round. Please refresh.</div>
                : isLoading || !data ? <LoadingSpinner fullPage className="gg-text-correct" />
                    : showSummary ? <GameSummary gameId={gameId} totalRoundCount={data.totalRoundCount} onOpenRound={goToRound} />
                        : renderGame(data)}
        </>
    );
}

export default GameRoundPage;
