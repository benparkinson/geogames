import { useRouter } from "next/router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AnswerState, UNANSWERED } from "../games/common/Model";
import { fetchJson, postJson } from "./http";
import { queryKeys } from "./queryKeys";
import { GameRoundResponse, GameRoundView } from "./types";

function roundUrl(gameId: string, round: number): string {
  return "/api/games/" + gameId + "/rounds/" + round;
}

export function toRoundView(response: GameRoundResponse, round: number): GameRoundView {
  return {
    gameType: response.gameType,
    data: JSON.parse(response.jsonBlob),
    totalRoundCount: response.totalRoundCount,
    answerState: response.answerState,
    gameResult: {
      gameState: response.gameResult.gameState,
      score: response.gameResult.score,
      roundAnswerStates: roundAnswerStates(response, round),
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
    mutationFn: (answerState: AnswerState) =>
      postJson<unknown>(roundUrl(gameId, round) + "/answers", "submit answer", { answerState }),
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
