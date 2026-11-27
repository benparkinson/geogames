import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/router";
import { fetchJson, putJson } from "./http";
import { queryKeys } from "./queryKeys";
import type {
  ChallengeCreationResponse,
  ChallengeResponse,
  ChallengeTableResponse,
  GameCreationResponse,
  InitialsRequest,
} from "./types";

/**
 * Makes a challenge from a finished game as soon as it is asked for, so the link exists before the player clicks
 * share (the clipboard needs a fresh click, which an awaited request would lose). It runs once per page load: the
 * key is the game, the result never goes stale and a failure isn't retried.
 */
export function useCreateChallenge(gameId: string, enabled = true) {
  return useQuery({
    queryKey: queryKeys.createChallenge(gameId),
    queryFn: () =>
      fetchJson<ChallengeCreationResponse>(`/api/games/${gameId}/challenges`, "create challenge", { method: "POST" }),
    enabled,
    staleTime: Infinity,
    gcTime: Infinity,
    retry: false,
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
  });
}

/** What a friend sees before playing a challenge. An expired or unknown one is a 404, which isn't retried. */
export function useChallenge(id: string | null) {
  return useQuery({
    queryKey: queryKeys.challenge(id ?? ""),
    queryFn: () => fetchJson<ChallengeResponse>(`/api/challenges/${id}`, "load challenge"),
    enabled: id !== null,
    retry: false,
    staleTime: 5 * 60_000,
  });
}

/** Starts a new game from a challenge's rounds and opens it. Each call makes a new game. */
export function useCreateChallengeGame() {
  const router = useRouter();
  return useMutation({
    mutationFn: (challengeId: string) =>
      fetchJson<GameCreationResponse>(`/api/challenges/${challengeId}/games`, "start challenge game", {
        method: "POST",
      }),
    onSuccess: (data) => {
      router.replace("/games/" + data.id);
    },
  });
}

/** The league table of a finished game's challenge. A game with no live challenge is a 404, which isn't retried. */
export function useChallengeTable(gameId: string, enabled: boolean) {
  return useQuery({
    queryKey: queryKeys.challengeTable(gameId),
    queryFn: () => fetchJson<ChallengeTableResponse>(`/api/games/${gameId}/challenge-table`, "load league table"),
    enabled,
    retry: false,
  });
}

/** Puts initials on the game's entry. The table is refetched afterwards, also when refused: the rank may have moved. */
export function useSetInitials(gameId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (initials: string) => {
      const body: InitialsRequest = { initials };
      return putJson(`/api/games/${gameId}/challenge-initials`, "save initials", body);
    },
    onSettled: () => queryClient.invalidateQueries({ queryKey: queryKeys.challengeTable(gameId) }),
  });
}
