import { useRouter } from "next/router";
import { QueryClient, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AnswerState, CORRECT_ANSWER, RoundAnswer, UNANSWERED } from "../games/common/Model";
import { isPerfect, MAX_ROUND_POINTS, roundPoints } from "../games/common/scoring";
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
    guesses: Array.isArray(response.guesses) ? response.guesses : [],
    hintsTakenAt: Array.isArray(response.hintsTakenAt) ? response.hintsTakenAt : [],
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
    // Round data only changes when this client answers, and an answer patches the cached rounds (see
    // useSubmitAnswer); only a failed answer refetches them.
    staleTime: Infinity,
  };
}

export function useRound(gameId: string, round: number) {
  return useQuery(roundQuery(gameId, round));
}

/** The answer response with every field the cache patch needs. A current backend always sends them all. */
export type CompleteAnswerResponse = RoundAnswerResponse & {
  hintsUsed: number;
  points: number;
  gameResult: GameResultResponse &
    Required<
      Pick<
        GameResultResponse,
        "maxScore" | "roundsCorrect" | "perfectRounds" | "roundAnswerStates" | "roundPoints" | "roundHintsUsed"
      >
    >;
};

/** Whether an answer response can be patched into the cache; an older backend or `{}` can't, and the game is refetched. */
export function isCompleteAnswerResponse(response: unknown): response is CompleteAnswerResponse {
  if (typeof response !== "object" || response === null) return false;
  const r = response as Record<string, unknown>;
  const result = r.gameResult;
  if (typeof result !== "object" || result === null) return false;
  const g = result as Record<string, unknown>;
  return (
    typeof r.answerState === "string" &&
    typeof r.hintsUsed === "number" &&
    typeof r.points === "number" &&
    typeof g.score === "number" &&
    typeof g.maxScore === "number" &&
    typeof g.roundsCorrect === "number" &&
    typeof g.perfectRounds === "number" &&
    Array.isArray(g.roundAnswerStates) &&
    Array.isArray(g.roundPoints) &&
    Array.isArray(g.roundHintsUsed)
  );
}

/**
 * `totalRoundCount` is known once the round has loaded; it decides whether there is a next round to prefetch.
 *
 * The answer's response carries the answered round's state and the whole game's result, and no round data. They
 * are patched into the cached rounds, so nothing is refetched. If the response isn't complete (an older backend),
 * or the server refused the answer (a round's answer is final), the cache is wrong and the game's rounds are refetched.
 */
export function useSubmitAnswer(gameId: string, round: number, totalRoundCount?: number) {
  const queryClient = useQueryClient();
  const invalidateGame = () => queryClient.invalidateQueries({ queryKey: queryKeys.game(gameId) });
  return useMutation({
    scope: roundWritesScope(gameId),
    mutationFn: (answer: RoundAnswer) =>
      postJson<RoundAnswerResponse>(roundUrl(gameId, round) + "/answers", "submit answer", answer),
    // Returning the promise keeps the mutation pending until the refetch or prefetch is done.
    onError: invalidateGame,
    onSuccess: async response => {
      if (isCompleteAnswerResponse(response)) {
        patchRounds(queryClient, gameId, round, response);
      } else {
        await invalidateGame();
      }
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
    onSuccess: response => {
      if (!Array.isArray(response?.guesses)) return;
      queryClient.setQueryData<GameRoundView>(
        queryKeys.round(gameId, round),
        old => old && { ...old, guesses: response.guesses },
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
    onSuccess: response => {
      if (!Array.isArray(response?.hintsTakenAt)) return;
      queryClient.setQueryData<GameRoundView>(
        queryKeys.round(gameId, round),
        old => old && { ...old, hintsTakenAt: response.hintsTakenAt },
      );
    },
    onError: () => queryClient.invalidateQueries({ queryKey: queryKeys.game(gameId) }),
  });
}

function patchRounds(
  queryClient: QueryClient,
  gameId: string,
  round: number,
  response: CompleteAnswerResponse,
) {
  const result = response.gameResult;
  const gameResult: GameRoundView["gameResult"] = {
    gameState: result.gameState,
    score: result.score,
    maxScore: result.maxScore,
    roundsCorrect: result.roundsCorrect,
    perfectRounds: result.perfectRounds,
    roundAnswerStates: result.roundAnswerStates,
    roundPoints: result.roundPoints,
    roundHintsUsed: result.roundHintsUsed,
  };
  // Every round of the game carries the whole game's result. Only round queries live under the game's key.
  queryClient.setQueriesData<GameRoundView>({ queryKey: queryKeys.game(gameId) }, old => old && { ...old, gameResult });
  queryClient.setQueryData<GameRoundView>(
    queryKeys.round(gameId, round),
    old =>
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
