import { act, renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { callsTo, mockFetch } from "../test/fetchMock";
import { makeRiverRound, makeRiverRoundData, makeRound } from "../test/fixtures";
import { clientWrapper, newTestClient } from "../test/render";
import type { AnswerState } from "../games/common/Model";
import { fetchRound, toRoundView, useCreateGame, useRound, useSubmitAnswer } from "./games";
import { queryKeys } from "./queryKeys";

const router = vi.hoisted(() => ({ push: vi.fn(), query: {} }));
vi.mock("next/router", () => ({ useRouter: () => router }));

describe("fetchRound", () => {
  it("fetches the round from the server and parses its jsonBlob", async () => {
    const fetchMock = mockFetch([
      { url: "http://localhost:8080/api/games/g1/rounds/3", body: makeRiverRound({ totalRoundCount: 5 }) },
    ]);

    const view = await fetchRound("g1", 3);

    expect(fetchMock).toHaveBeenCalledWith("http://localhost:8080/api/games/g1/rounds/3");
    expect(view.data).toEqual(makeRiverRoundData());
    expect(view.totalRoundCount).toBe(5);
    expect(view.gameType).toEqual({ name: "RIVERS_BY_SHAPE", answerCount: 1, hintCount: 1 });
  });

  it("rejects with the round and status when the response is not ok", async () => {
    mockFetch([{ url: /rounds\/3$/, status: 404, body: {} }]);

    await expect(fetchRound("g1", 3)).rejects.toThrow("Failed to load round 3 (404)");
  });
});

describe("toRoundView", () => {
  it("passes the server's answer states through", () => {
    const view = toRoundView(
      makeRound({
        gameResult: {
          gameState: "IN_PROGRESS",
          roundsUnanswered: 1,
          score: 1,
          roundAnswerStates: ["CORRECT", "UNANSWERED"],
        },
      }),
      0,
    );

    expect(view.gameResult.roundAnswerStates).toEqual(["CORRECT", "UNANSWERED"]);
  });

  it("passes the server's points and hints through", () => {
    const view = toRoundView(
      makeRound({
        answerState: "CORRECT",
        hintsUsed: 1,
        points: 83,
        gameResult: {
          gameState: "IN_PROGRESS",
          roundsUnanswered: 1,
          score: 183,
          roundAnswerStates: ["CORRECT", "CORRECT", "UNANSWERED"],
          maxScore: 300,
          roundsCorrect: 2,
          perfectRounds: 1,
          roundPoints: [100, 83, 0],
          roundHintsUsed: [0, 1, 0],
        },
      }),
      1,
    );

    expect(view.hintsUsed).toBe(1);
    expect(view.points).toBe(83);
    expect(view.gameType.hintCount).toBe(3);
    expect(view.gameResult).toEqual({
      gameState: "IN_PROGRESS",
      score: 183,
      maxScore: 300,
      roundsCorrect: 2,
      perfectRounds: 1,
      roundAnswerStates: ["CORRECT", "CORRECT", "UNANSWERED"],
      roundPoints: [100, 83, 0],
      roundHintsUsed: [0, 1, 0],
    });
  });

  it("works out the points itself when an older backend sends none", () => {
    const response = makeRound({
      totalRoundCount: 3,
      answerState: "CORRECT",
      hintsUsed: undefined,
      points: undefined,
      gameType: { name: "TRIPOINT", answerCount: 3 },
      gameResult: {
        gameState: "IN_PROGRESS",
        roundsUnanswered: 1,
        // The old score counted correct rounds, so it must not be used as points.
        score: 2,
        roundAnswerStates: ["CORRECT", "CORRECT", "UNANSWERED"],
      },
    });

    const view = toRoundView(response, 1);

    expect(view.gameType.hintCount).toBe(0);
    expect(view.hintsUsed).toBe(0);
    expect(view.points).toBe(100);
    expect(view.gameResult).toEqual({
      gameState: "IN_PROGRESS",
      score: 200,
      maxScore: 300,
      roundsCorrect: 2,
      perfectRounds: 2,
      roundAnswerStates: ["CORRECT", "CORRECT", "UNANSWERED"],
      roundPoints: [100, 100, 0],
      roundHintsUsed: [0, 0, 0],
    });
  });

  it("falls back to this round's state when an older backend sends no roundAnswerStates", () => {
    const response = makeRound({ totalRoundCount: 3, answerState: "GAVE_UP" });
    delete response.gameResult.roundAnswerStates;

    expect(toRoundView(response, 1).gameResult.roundAnswerStates).toEqual(["UNANSWERED", "GAVE_UP", "UNANSWERED"]);
  });
});

describe("useRound", () => {
  it("loads and caches the round under its query key", async () => {
    mockFetch([{ url: /\/rounds\/2$/, body: makeRound() }]);
    const client = newTestClient();
    const { result } = renderHook(() => useRound("g1", 2), { wrapper: clientWrapper(client) });

    await waitFor(() => expect(result.current.data?.totalRoundCount).toBe(3));
    expect(client.getQueryData(queryKeys.round("g1", 2))).toBeDefined();
  });
});

describe("useSubmitAnswer", () => {
  it("posts the answer state and refetches that round", async () => {
    let state = "UNANSWERED";
    const fetchMock = mockFetch([
      { url: /\/rounds\/0$/, body: () => makeRound({ answerState: state as AnswerState }) },
      {
        method: "POST",
        url: "http://localhost:8080/api/games/g1/rounds/0/answers",
        body: ({ init }) => {
          state = JSON.parse(init!.body as string).answerState;
          return makeRound({ answerState: state as AnswerState });
        },
      },
    ]);
    const { result } = renderHook(() => ({ round: useRound("g1", 0), submit: useSubmitAnswer("g1", 0) }), {
      wrapper: clientWrapper(),
    });
    await waitFor(() => expect(result.current.round.data?.answerState).toBe("UNANSWERED"));

    act(() => result.current.submit.mutate({ answerState: "GAVE_UP", hintsUsed: 2 }));

    await waitFor(() => expect(result.current.round.data?.answerState).toBe("GAVE_UP"));
    const posts = callsTo(fetchMock).filter((c) => c.method === "POST");
    expect(posts).toHaveLength(1);
    expect(posts[0].body).toEqual({ answerState: "GAVE_UP", hintsUsed: 2 });
  });
});

describe("useCreateGame", () => {
  it("posts the request and navigates to the new game", async () => {
    router.push.mockClear();
    const fetchMock = mockFetch([
      { method: "POST", url: "http://localhost:8080/api/games", body: { id: 7 } },
    ]);
    const { result } = renderHook(() => useCreateGame(), { wrapper: clientWrapper() });

    act(() => result.current.createGame("TRIPOINT", 5));

    await waitFor(() => expect(router.push).toHaveBeenCalledWith("/games/7"));
    const [call] = callsTo(fetchMock);
    expect(call.method).toBe("POST");
    expect(call.body).toEqual({ gameType: "TRIPOINT", numberOfRounds: 5 });
    expect(new Headers(call.init?.headers).get("Content-Type")).toBe("application/json");
  });

  it("is loading while the request is pending", async () => {
    let finish: (value: unknown) => void = () => {};
    vi.stubGlobal(
      "fetch",
      vi.fn(() => new Promise((resolve) => (finish = resolve))),
    );
    const { result } = renderHook(() => useCreateGame(), { wrapper: clientWrapper() });
    expect(result.current.isLoading).toBe(false);

    act(() => result.current.createGame("RANDOM", 3));
    await waitFor(() => expect(result.current.isLoading).toBe(true));

    await act(async () => finish({ ok: true, status: 200, json: async () => ({ id: 1 }) }));
    await waitFor(() => expect(result.current.isLoading).toBe(false));
  });

  it("reports an error and does not navigate when the server refuses", async () => {
    router.push.mockClear();
    mockFetch([{ method: "POST", url: /\/api\/games$/, status: 500, body: {} }]);
    const { result } = renderHook(() => useCreateGame(), { wrapper: clientWrapper() });

    act(() => result.current.createGame("TRIPOINT", 5));

    await waitFor(() => expect(result.current.error).toBeTruthy());
    expect(String(result.current.error)).toContain("Failed to create game (500)");
    expect(router.push).not.toHaveBeenCalled();
  });
});
