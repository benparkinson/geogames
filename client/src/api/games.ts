import { useRouter } from "next/router";
import { useMutation } from "@tanstack/react-query";
import { GameRoundModel } from "../games/common/Model";
import { serverEndpoint } from "./gateway";

export function roundUrl(gameId: string, round: number): string {
  return "/api/games/" + gameId + "/rounds/" + round;
}

export async function fetchRound(gameId: string, round: number): Promise<GameRoundModel> {
  const response = await fetch(serverEndpoint() + roundUrl(gameId, round));
  if (!response.ok) {
    throw new Error("Failed to load round " + round + " (" + response.status + ")");
  }
  return response.json();
}

export function useCreateGame(): {
  createGame: (gameType: string, numberOfRounds: number) => void;
  isLoading: boolean;
  error: unknown;
} {
  const router = useRouter();
  const { mutate, isPending, error } = useMutation({
    mutationFn: async (request: { gameType: string; numberOfRounds: number }) => {
      const response = await fetch(serverEndpoint() + "/api/games", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(request),
      });
      if (!response.ok) {
        throw new Error("Failed to create game (" + response.status + ")");
      }
      return response.json() as Promise<{ id: number }>;
    },
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
