import Head from "next/head";
import { useEffect, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { roundQuery, useRecordGuess, useRecordHint, useRound, useSubmitAnswer } from "../../api/games";
import { GameRoundView } from "../../api/types";
import type { DailyInfo } from "../../daily/dailyStorage";
import GameSummary from "./GameSummary";
import { FINISHED, Round, RoundAnswer, UNANSWERED } from "./Model";
import LoadingSpinner from "../../components/LoadingSpinner";
import { findGameModule } from "../registry";

/** The callbacks of the page that a `Round` hands to the game. */
type RoundActions = Pick<
  Round,
  "answerSave" | "retryAnswer" | "nextRound" | "goToRound" | "showSummary" | "recordHint" | "recordGuess"
>;

/** What a game component is told about its round: the server's view of it, and what the player can do next. */
function buildRound(
  data: GameRoundView,
  currentRound: number,
  kind: Round["kind"],
  hasClues: boolean,
  dailyDate: string | undefined,
  actions: RoundActions,
): Round {
  return {
    ...actions,
    kind,
    hasNextRound: currentRound < data.totalRoundCount - 1,
    currentRound,
    totalRoundCount: data.totalRoundCount,
    answerState: data.answerState,
    hintCount: data.gameType.hintCount,
    guessLimit: data.gameType.guessLimit,
    hasClues,
    hintsUsed: data.hintsUsed,
    guesses: data.guesses,
    hintsTakenAt: data.hintsTakenAt,
    points: data.points,
    score: data.gameResult.score,
    maxScore: data.gameResult.maxScore,
    perfectRounds: data.gameResult.perfectRounds,
    gameCompleted: data.gameResult.gameState === FINISHED,
    roundAnswerStates: data.gameResult.roundAnswerStates,
    roundPoints: data.gameResult.roundPoints,
    dailyDate,
  };
}

/** `daily` is set when the game is a daily challenge: the title says so, and the summary shares instead of replaying. */
function GameRoundPage({ gameId, daily }: { gameId: string; daily?: DailyInfo }): React.JSX.Element {
  // Until the player acts, resume at the first unanswered round, which the first round's game result tells us.
  const [chosenRound, setRound] = useState<number | null>(null);
  const [summaryChoice, setSummaryChoice] = useState<boolean | null>(null);
  const firstRound = useRound(gameId, 0).data;
  // Until the player acts, a finished game opens on its results rather than on the first round.
  const showSummary = summaryChoice ?? (chosenRound === null && firstRound?.gameResult.gameState === FINISHED);
  const resumeRound = Math.max(0, firstRound?.gameResult.roundAnswerStates.indexOf(UNANSWERED) ?? 0);
  const currentRound = chosenRound ?? resumeRound;
  const { error, data, isLoading } = useRound(gameId, currentRound);
  // A mixed game's rounds can differ in type, so this is looked up for the round being shown.
  const gameModule = data ? findGameModule(data.gameType.name) : undefined;
  const submitAnswerMutation = useSubmitAnswer(gameId, currentRound, data?.totalRoundCount);
  const recordGuessMutation = useRecordGuess(gameId, currentRound);
  const recordHintMutation = useRecordHint(gameId, currentRound);
  const queryClient = useQueryClient();

  // A round answered before this page loaded: its next round is the likely next click. Does nothing when cached.
  const answered = data !== undefined && data.answerState !== UNANSWERED;
  const totalRoundCount = data?.totalRoundCount ?? 0;
  useEffect(() => {
    if (answered && currentRound + 1 < totalRoundCount) {
      void queryClient.prefetchQuery(roundQuery(gameId, currentRound + 1));
    }
  }, [answered, currentRound, totalRoundCount, gameId, queryClient]);

  // Leaving a round before its answer is saved would show it as unanswered (a stale fetch of the next round can
  // also overwrite the patched game result), so every way out waits for the save.
  const answerSave: Round["answerSave"] = submitAnswerMutation.isError
    ? "failed"
    : submitAnswerMutation.isPending
      ? "saving"
      : "saved";

  function whenSaved(action: () => void): void {
    if (answerSave === "saved") {
      action();
    }
  }

  function nextRound(): void {
    whenSaved(() => setRound(currentRound + 1));
  }

  function goToRound(round: number): void {
    whenSaved(() => {
      setRound(round);
      setSummaryChoice(false);
    });
  }

  function showSummaryPage(): void {
    whenSaved(() => setSummaryChoice(true));
  }

  function retryAnswer(): void {
    // Only offered once the answer failed, so the mutation still holds its request.
    submitAnswerMutation.mutate(submitAnswerMutation.variables as RoundAnswer);
  }

  function submitAnswer(answer: RoundAnswer): void {
    // Pin the round, or the refreshed game result would move the resume point on by itself.
    setRound(currentRound);
    submitAnswerMutation.mutate(answer);
  }

  function renderGame(data: GameRoundView) {
    if (!gameModule) {
      return <div>Unknown game type...</div>;
    }
    const kind = { title: gameModule.title, Icon: gameModule.Icon };
    const round = buildRound(data, currentRound, kind, !!gameModule.hasClues, daily?.date, {
      answerSave,
      retryAnswer,
      nextRound,
      goToRound,
      showSummary: showSummaryPage,
      recordHint: (hint) => recordHintMutation.mutate(hint),
      recordGuess: (guess) => recordGuessMutation.mutate(guess),
    });

    // The key resets the round's state, and the game type can change between rounds of a mixed game.
    return (
      <gameModule.Component
        key={currentRound}
        data={data.data}
        round={round}
        answerCount={data.gameType.answerCount}
        submitAnswer={submitAnswer}
      />
    );
  }

  function renderTitle() {
    if (!data) {
      return <title>Loading... | GeoGauntlet</title>;
    }
    if (!gameModule) {
      return <title>Unknown game! | GeoGauntlet</title>;
    }
    if (daily) {
      return <title>{`Daily ${daily.date} | GeoGauntlet`}</title>;
    }
    return <title>{`${gameModule.title} | GeoGauntlet`}</title>;
  }

  return (
    <>
      <Head>{renderTitle()}</Head>
      {error ? (
        <div className="p-4 gg-text-danger" role="alert">
          Couldn&apos;t load this round. Please refresh.
        </div>
      ) : isLoading || !data ? (
        <LoadingSpinner fullPage className="gg-text-correct" />
      ) : showSummary ? (
        <GameSummary gameId={gameId} totalRoundCount={data.totalRoundCount} onOpenRound={goToRound} daily={daily} />
      ) : (
        renderGame(data)
      )}
    </>
  );
}

export default GameRoundPage;
