import { useRouter } from "next/router";
import { QueryClient, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { RoundAnswer } from "../games/common/Model";
import { fetchJson, postJson } from "./http";
import { queryKeys } from "./queryKeys";
import {
  GameResultResponse,
  GameRoundResponse,
  GameRoundView,
  RecordedGuess,
  RoundAnswerResponse,
  RoundGuessesResponse,
  RoundHintsResponse,
} from "./types";

/** Mutations in one scope run one after another, so an answer reaches the server after the guess that led to it. */
function roundWritesScope(gameId: string) {
  return { id: "round-writes:" + gameId };
}

function roundUrl(gameId: string, round: number): string {
  return "/api/games/" + gameId + "/rounds/" + round;
}

function toGameResultView(result: GameResultResponse): GameRoundView["gameResult"] {
  const { roundsUnanswered: _roundsUnanswered, ...view } = result;
  return view;
}

export function toRoundView(response: GameRoundResponse): GameRoundView {
  return {
    gameType: response.gameType,
    data: JSON.parse(response.jsonBlob),
    totalRoundCount: response.totalRoundCount,
    answerState: response.answerState,
    hintsUsed: response.hintsUsed,
    points: response.points,
    guesses: response.guesses,
    hintsTakenAt: response.hintsTakenAt,
    gameResult: toGameResultView(response.gameResult),
  };
}

export async function fetchRound(gameId: string, round: number): Promise<GameRoundView> {
  const response = await fetchJson<GameRoundResponse>(roundUrl(gameId, round), `load round ${round}`);
  return toRoundView(response);
}

export function roundQuery(gameId: string, round: number) {
  return {
    queryKey: queryKeys.round(gameId, round),
    queryFn: () => fetchRound(gameId, round),
    // Round data only changes when this client answers, and an answer patches the cached rounds (see
    // useSubmitAnswer); a failed answer, guess or hint refetches the game.
    staleTime: Infinity,
  };
}

export function useRound(gameId: string, round: number) {
  return useQuery(roundQuery(gameId, round));
}

/**
 * `totalRoundCount` is known once the round has loaded; it decides whether there is a next round to prefetch.
 *
 * The answer's response carries the answered round's state and the whole game's result, and no round data. They
 * are patched into the cached rounds, so nothing is refetched. If the server refused the answer (a round's answer
 * is final), the cache is wrong and the game's rounds are refetched.
 */
export function useSubmitAnswer(gameId: string, round: number, totalRoundCount?: number) {
  const queryClient = useQueryClient();
  const invalidateGame = () => queryClient.invalidateQueries({ queryKey: queryKeys.game(gameId) });
  return useMutation({
    scope: roundWritesScope(gameId),
    mutationFn: (answer: RoundAnswer) =>
      postJson<RoundAnswerResponse>(roundUrl(gameId, round) + "/answers", "submit answer", answer),
    // Returning the promise keeps the mutation pending until the refetch (onError) or the prefetch (onSuccess) is done.
    onError: invalidateGame,
    onSuccess: async (response) => {
      patchRounds(queryClient, gameId, round, response);
      // Only now, so the next round's score isn't already out of date.
      if (totalRoundCount !== undefined && round + 1 < totalRoundCount) {
        await queryClient.prefetchQuery(roundQuery(gameId, round + 1));
      }
    },
  });
}

/**
 * Saves a guess the guess reducer recorded, so a refresh restores it. The response's guesses are patched into
 * the cached round: round queries never go stale, so without it a round reopened from the cache would have none.
 */
export function useRecordGuess(gameId: string, round: number) {
  const queryClient = useQueryClient();
  return useMutation({
    scope: roundWritesScope(gameId),
    mutationFn: (guess: RecordedGuess) =>
      postJson<RoundGuessesResponse>(roundUrl(gameId, round) + "/guesses", "save guess", {
        guess: guess.text,
        correct: guess.correct,
      }),
    onSuccess: (response) => {
      queryClient.setQueryData<GameRoundView>(
        queryKeys.round(gameId, round),
        (old) => old && { ...old, guesses: response.guesses },
      );
    },
    onError: () => queryClient.invalidateQueries({ queryKey: queryKeys.game(gameId) }),
  });
}

/**
 * Saves that a hint was taken, with how many guesses had been made before it, so a refresh keeps it. The hints are
 * patched into the cached round for the same reason as the guesses.
 */
export function useRecordHint(gameId: string, round: number) {
  const queryClient = useQueryClient();
  return useMutation({
    scope: roundWritesScope(gameId),
    mutationFn: (hint: { hint: number; guessesBefore: number }) =>
      postJson<RoundHintsResponse>(roundUrl(gameId, round) + "/hints", "save hint", hint),
    onSuccess: (response) => {
      queryClient.setQueryData<GameRoundView>(
        queryKeys.round(gameId, round),
        (old) => old && { ...old, hintsTakenAt: response.hintsTakenAt },
      );
    },
    onError: () => queryClient.invalidateQueries({ queryKey: queryKeys.game(gameId) }),
  });
}

function patchRounds(queryClient: QueryClient, gameId: string, round: number, response: RoundAnswerResponse) {
  const gameResult = toGameResultView(response.gameResult);
  // Every round of the game carries the whole game's result. Only round queries live under the game's key.
  queryClient.setQueriesData<GameRoundView>(
    { queryKey: queryKeys.game(gameId) },
    (old) => old && { ...old, gameResult },
  );
  queryClient.setQueryData<GameRoundView>(
    queryKeys.round(gameId, round),
    (old) =>
      old && {
        ...old,
        answerState: response.answerState,
        hintsUsed: response.hintsUsed,
        points: response.points,
      },
  );
}

export function useCreateGame(): {
  createGame: (gameType: string, numberOfRounds: number) => void;
  isLoading: boolean;
  error: unknown;
} {
  const router = useRouter();
  const { mutate, isPending, error } = useMutation({
    mutationFn: (request: { gameType: string; numberOfRounds: number }) =>
      postJson<{ id: string }>("/api/games", "create game", request),
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
