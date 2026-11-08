import { useRouter } from "next/router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AnswerState, CORRECT_ANSWER, RoundAnswer, UNANSWERED } from "../games/common/Model";
import { isPerfect, MAX_ROUND_POINTS, roundPoints } from "../games/common/scoring";
import { fetchJson, postJson } from "./http";
import { queryKeys } from "./queryKeys";
import { GameRoundResponse, GameRoundView } from "./types";

function roundUrl(gameId: string, round: number): string {
  return "/api/games/" + gameId + "/rounds/" + round;
}

export function toRoundView(response: GameRoundResponse, round: number): GameRoundView {
  const result = response.gameResult;
  const states = roundAnswerStates(response, round);
  const serverPoints = result.roundPoints;
  // Older backends send none of the points fields, and their score counted correct rounds.
  const points = Array.isArray(serverPoints) ? serverPoints : states.map(state => roundPoints(state, 0, 0));
  const hintsUsed = Array.isArray(result.roundHintsUsed) ? result.roundHintsUsed : states.map(() => 0);
  return {
    gameType: {
      ...response.gameType,
      hintCount: response.gameType.hintCount ?? 0,
      // An older backend has no limit.
      guessLimit: response.gameType.guessLimit ?? Infinity,
    },
    data: JSON.parse(response.jsonBlob),
    totalRoundCount: response.totalRoundCount,
    answerState: response.answerState,
    hintsUsed: response.hintsUsed ?? 0,
    points: response.points ?? roundPoints(response.answerState, 0, 0),
    gameResult: {
      gameState: result.gameState,
      score: Array.isArray(serverPoints) ? result.score : points.reduce((sum, p) => sum + p, 0),
      maxScore: result.maxScore ?? MAX_ROUND_POINTS * response.totalRoundCount,
      roundsCorrect: result.roundsCorrect ?? states.filter(s => s === CORRECT_ANSWER).length,
      perfectRounds:
        result.perfectRounds ?? states.filter((s, i) => isPerfect(s, hintsUsed[i] ?? 0)).length,
      roundAnswerStates: states,
      roundPoints: points,
      roundHintsUsed: hintsUsed,
    },
  };
}

// Older backends don't send roundAnswerStates; fall back to what this response tells us.
function roundAnswerStates(response: GameRoundResponse, round: number): AnswerState[] {
  if (Array.isArray(response.gameResult?.roundAnswerStates)) {
    return response.gameResult.roundAnswerStates;
  }
  const states: AnswerState[] = Array(response.totalRoundCount).fill(UNANSWERED);
  states[round] = response.answerState;
  return states;
}

export async function fetchRound(gameId: string, round: number): Promise<GameRoundView> {
  const response = await fetchJson<GameRoundResponse>(roundUrl(gameId, round), `load round ${round}`);
  return toRoundView(response, round);
}

export function roundQuery(gameId: string, round: number) {
  return {
    queryKey: queryKeys.round(gameId, round),
    queryFn: () => fetchRound(gameId, round),
  };
}

export function useRound(gameId: string, round: number) {
  return useQuery(roundQuery(gameId, round));
}

export function useSubmitAnswer(gameId: string, round: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (answer: RoundAnswer) =>
      postJson<unknown>(roundUrl(gameId, round) + "/answers", "submit answer", answer),
    // Refetch on failure too: if the server refused the answer (a round's answer is final), show what it holds.
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.round(gameId, round) });
    },
  });
}

export function useCreateGame(): {
  createGame: (gameType: string, numberOfRounds: number) => void;
  isLoading: boolean;
  error: unknown;
} {
  const router = useRouter();
  const { mutate, isPending, error } = useMutation({
    mutationFn: (request: { gameType: string; numberOfRounds: number }) =>
      postJson<{ id: number }>("/api/games", "create game", request),
    onSuccess: (data) => {
      router.push("/games/" + data.id);
    },
  });

  return {
    createGame: (gameType, numberOfRounds) => mutate({ gameType, numberOfRounds }),
    isLoading: isPending,
    error,
  };
}
