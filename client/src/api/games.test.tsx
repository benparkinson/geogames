import { act, renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { callsTo, mockFetch } from "../test/fetchMock";
import {
  makeAnswerResponse,
  makeGuessesResponse,
  makeRiverRound,
  makeRiverRoundData,
  makeRound,
} from "../test/fixtures";
import { clientWrapper, newTestClient } from "../test/render";
import {
  fetchRound,
  toRoundView,
  useCreateGame,
  useRecordGuess,
  useRecordHint,
  useRound,
  useSubmitAnswer,
} from "./games";
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
    expect(view.gameType).toEqual({ name: "RIVERS_BY_SHAPE", answerCount: 1, hintCount: 3, guessLimit: 5 });
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
          ...makeRound().gameResult,
          gameState: "IN_PROGRESS",
          roundsUnanswered: 1,
          score: 1,
          roundAnswerStates: ["CORRECT", "UNANSWERED"],
        },
      }),
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
          roundFirstGuess: [true, false, false],
          firstGuessRounds: 1,
        },
      }),
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
      roundFirstGuess: [true, false, false],
      firstGuessRounds: 1,
    });
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
  const roundGets = (fetchMock: ReturnType<typeof mockFetch>, round: number) =>
    callsTo(fetchMock).filter((c) => c.method === "GET" && c.url.endsWith(`/rounds/${round}`));

  const answered = makeAnswerResponse({ hintsUsed: 2, points: 83 });

  function setup(post: object | (() => object) = answered, status = 200) {
    return mockFetch([
      { url: /\/rounds\/\d$/, body: () => makeRound({ totalRoundCount: 2 }) },
      { method: "POST", url: /\/answers$/, body: post, status },
    ]);
  }

  it("posts the answer and patches the answered round without fetching it again", async () => {
    const fetchMock = setup();
    const { result } = renderHook(() => ({ round: useRound("g1", 0), submit: useSubmitAnswer("g1", 0) }), {
      wrapper: clientWrapper(),
    });
    await waitFor(() => expect(result.current.round.data?.answerState).toBe("UNANSWERED"));

    act(() => result.current.submit.mutate({ answerState: "GAVE_UP", hintsUsed: 2, wrongGuesses: 0, answersFound: 0 }));

    await waitFor(() => expect(result.current.round.data?.answerState).toBe("CORRECT"));
    const view = result.current.round.data!;
    expect(view.hintsUsed).toBe(2);
    expect(view.points).toBe(83);
    expect(view.gameResult).toEqual({
      gameState: answered.gameResult.gameState,
      score: answered.gameResult.score,
      maxScore: answered.gameResult.maxScore,
      roundsCorrect: answered.gameResult.roundsCorrect,
      perfectRounds: answered.gameResult.perfectRounds,
      roundAnswerStates: answered.gameResult.roundAnswerStates,
      roundPoints: answered.gameResult.roundPoints,
      roundHintsUsed: answered.gameResult.roundHintsUsed,
      roundFirstGuess: answered.gameResult.roundFirstGuess,
      firstGuessRounds: answered.gameResult.firstGuessRounds,
    });
    const posts = callsTo(fetchMock).filter((c) => c.method === "POST");
    expect(posts).toHaveLength(1);
    expect(posts[0].body).toEqual({ answerState: "GAVE_UP", hintsUsed: 2, wrongGuesses: 0, answersFound: 0 });
    expect(roundGets(fetchMock, 0)).toHaveLength(1);
  });

  it("gives the game's other cached rounds the new result and keeps their own state and data", async () => {
    const fetchMock = setup();
    const { result } = renderHook(
      () => ({ r0: useRound("g1", 0), r1: useRound("g1", 1), submit: useSubmitAnswer("g1", 0, 2) }),
      { wrapper: clientWrapper() },
    );
    await waitFor(() => expect(result.current.r1.data).toBeDefined());
    const before = result.current.r1.data!;

    act(() => result.current.submit.mutate({ answerState: "CORRECT", hintsUsed: 2, wrongGuesses: 0, answersFound: 1 }));

    await waitFor(() => expect(result.current.submit.isSuccess).toBe(true));
    const after = result.current.r1.data!;
    expect(after.gameResult.roundAnswerStates).toEqual(answered.gameResult.roundAnswerStates);
    expect(after.gameResult.score).toBe(answered.gameResult.score);
    expect(after.answerState).toBe(before.answerState);
    expect(after.data).toBe(before.data);
    expect(roundGets(fetchMock, 0)).toHaveLength(1);
    expect(roundGets(fetchMock, 1)).toHaveLength(1);
  });

  it("prefetches the next round when nothing has cached it", async () => {
    const fetchMock = setup();
    const { result } = renderHook(() => ({ r0: useRound("g1", 0), submit: useSubmitAnswer("g1", 0, 2) }), {
      wrapper: clientWrapper(),
    });
    await waitFor(() => expect(result.current.r0.data).toBeDefined());
    expect(roundGets(fetchMock, 1)).toHaveLength(0);

    act(() => result.current.submit.mutate({ answerState: "CORRECT", hintsUsed: 0, wrongGuesses: 0, answersFound: 1 }));

    await waitFor(() => expect(roundGets(fetchMock, 1)).toHaveLength(1));
  });

  it("fetches nothing beyond the last round", async () => {
    const fetchMock = setup();
    const { result } = renderHook(() => ({ r1: useRound("g1", 1), submit: useSubmitAnswer("g1", 1, 2) }), {
      wrapper: clientWrapper(),
    });
    await waitFor(() => expect(result.current.r1.data).toBeDefined());

    act(() => result.current.submit.mutate({ answerState: "CORRECT", hintsUsed: 0, wrongGuesses: 0, answersFound: 1 }));

    await waitFor(() => expect(result.current.submit.isSuccess).toBe(true));
    expect(roundGets(fetchMock, 2)).toHaveLength(0);
  });

  it("refetches the game's cached rounds when the server refuses the answer", async () => {
    const fetchMock = setup({ title: "Conflict" }, 409);
    const { result } = renderHook(
      () => ({ r0: useRound("g1", 0), r1: useRound("g1", 1), submit: useSubmitAnswer("g1", 0, 2) }),
      { wrapper: clientWrapper() },
    );
    await waitFor(() => expect(result.current.r1.data).toBeDefined());

    act(() => result.current.submit.mutate({ answerState: "CORRECT", hintsUsed: 0, wrongGuesses: 0, answersFound: 1 }));

    await waitFor(() => expect(result.current.submit.isError).toBe(true));
    await waitFor(() => expect(roundGets(fetchMock, 0)).toHaveLength(2));
    await waitFor(() => expect(roundGets(fetchMock, 1)).toHaveLength(2));
  });
});

describe("useRecordGuess", () => {
  const guessesAfter = [
    { text: "France", correct: false },
    { text: "Spain", correct: true },
  ];

  it("posts the guess and patches the saved guesses into the cached round", async () => {
    const fetchMock = mockFetch([
      { url: /\/rounds\/\d$/, body: () => makeRound({ guesses: [] }) },
      { method: "POST", url: /\/guesses$/, body: makeGuessesResponse({ guesses: guessesAfter }) },
    ]);
    const { result } = renderHook(() => ({ round: useRound("g1", 0), record: useRecordGuess("g1", 0) }), {
      wrapper: clientWrapper(),
    });
    await waitFor(() => expect(result.current.round.data?.guesses).toEqual([]));

    act(() => result.current.record.mutate({ text: "Spain", correct: true }));

    await waitFor(() => expect(result.current.round.data?.guesses).toEqual(guessesAfter));
    const posts = callsTo(fetchMock).filter((c) => c.method === "POST");
    expect(posts).toHaveLength(1);
    expect(posts[0].url).toBe("http://localhost:8080/api/games/g1/rounds/0/guesses");
    expect(posts[0].body).toEqual({ guess: "Spain", correct: true });
    expect(callsTo(fetchMock).filter((c) => c.method === "GET")).toHaveLength(1);
  });

  it("refetches the game when the guess is refused", async () => {
    const fetchMock = mockFetch([
      { url: /\/rounds\/\d$/, body: () => makeRound() },
      { method: "POST", url: /\/guesses$/, body: {}, status: 409 },
    ]);
    const { result } = renderHook(() => ({ round: useRound("g1", 0), record: useRecordGuess("g1", 0) }), {
      wrapper: clientWrapper(),
    });
    await waitFor(() => expect(result.current.round.data).toBeDefined());

    act(() => result.current.record.mutate({ text: "Spain", correct: false }));

    await waitFor(() => expect(callsTo(fetchMock).filter((c) => c.method === "GET")).toHaveLength(2));
  });

  it("sends a guess and the answer it led to in that order", async () => {
    const fetchMock = mockFetch([
      { url: /\/rounds\/\d$/, body: () => makeRound({ totalRoundCount: 1 }) },
      { method: "POST", url: /\/guesses$/, body: makeGuessesResponse() },
      { method: "POST", url: /\/answers$/, body: makeAnswerResponse() },
    ]);
    const { result } = renderHook(
      () => ({ round: useRound("g1", 0), record: useRecordGuess("g1", 0), submit: useSubmitAnswer("g1", 0, 1) }),
      { wrapper: clientWrapper() },
    );
    await waitFor(() => expect(result.current.round.data).toBeDefined());

    act(() => {
      result.current.record.mutate({ text: "France", correct: true });
      result.current.submit.mutate({ answerState: "CORRECT", hintsUsed: 0, wrongGuesses: 0, answersFound: 1 });
    });

    await waitFor(() => expect(result.current.round.data?.answerState).toBe("CORRECT"));
    const posts = callsTo(fetchMock).filter((c) => c.method === "POST");
    expect(posts.map((c) => c.url.split("/").pop())).toEqual(["guesses", "answers"]);
  });
});

describe("useRecordHint", () => {
  it("posts the hint and patches the saved hints into the cached round", async () => {
    const fetchMock = mockFetch([
      { url: /\/rounds\/\d$/, body: () => makeRound() },
      { method: "POST", url: /\/hints$/, body: { hintsTakenAt: [0, 2] } },
    ]);
    const { result } = renderHook(() => ({ round: useRound("g1", 0), record: useRecordHint("g1", 0) }), {
      wrapper: clientWrapper(),
    });
    await waitFor(() => expect(result.current.round.data?.hintsTakenAt).toEqual([]));

    act(() => result.current.record.mutate({ hint: 1, guessesBefore: 2 }));

    await waitFor(() => expect(result.current.round.data?.hintsTakenAt).toEqual([0, 2]));
    const posts = callsTo(fetchMock).filter((c) => c.method === "POST");
    expect(posts).toHaveLength(1);
    expect(posts[0].url).toBe("http://localhost:8080/api/games/g1/rounds/0/hints");
    expect(posts[0].body).toEqual({ hint: 1, guessesBefore: 2 });
  });

  it("refetches the game when the hint is refused", async () => {
    const fetchMock = mockFetch([
      { url: /\/rounds\/\d$/, body: () => makeRound() },
      { method: "POST", url: /\/hints$/, body: {}, status: 409 },
    ]);
    const { result } = renderHook(() => ({ round: useRound("g1", 0), record: useRecordHint("g1", 0) }), {
      wrapper: clientWrapper(),
    });
    await waitFor(() => expect(result.current.round.data).toBeDefined());

    act(() => result.current.record.mutate({ hint: 0, guessesBefore: 0 }));

    await waitFor(() => expect(callsTo(fetchMock).filter((c) => c.method === "GET")).toHaveLength(2));
  });

  it("sends a hint, a guess and the answer in the order they were made", async () => {
    const fetchMock = mockFetch([
      { url: /\/rounds\/\d$/, body: () => makeRound({ totalRoundCount: 1 }) },
      { method: "POST", url: /\/hints$/, body: { hintsTakenAt: [0] } },
      { method: "POST", url: /\/guesses$/, body: makeGuessesResponse() },
      { method: "POST", url: /\/answers$/, body: makeAnswerResponse() },
    ]);
    const { result } = renderHook(
      () => ({
        round: useRound("g1", 0),
        hint: useRecordHint("g1", 0),
        guess: useRecordGuess("g1", 0),
        submit: useSubmitAnswer("g1", 0, 1),
      }),
      { wrapper: clientWrapper() },
    );
    await waitFor(() => expect(result.current.round.data).toBeDefined());

    act(() => {
      result.current.hint.mutate({ hint: 0, guessesBefore: 0 });
      result.current.guess.mutate({ text: "France", correct: true });
      result.current.submit.mutate({ answerState: "CORRECT", hintsUsed: 1, wrongGuesses: 0, answersFound: 1 });
    });

    await waitFor(() => expect(result.current.round.data?.answerState).toBe("CORRECT"));
    const posts = callsTo(fetchMock).filter((c) => c.method === "POST");
    expect(posts.map((c) => c.url.split("/").pop())).toEqual(["hints", "guesses", "answers"]);
  });
});

describe("useCreateGame", () => {
  it("posts the request and navigates to the new game", async () => {
    router.push.mockClear();
    const fetchMock = mockFetch([
      { method: "POST", url: "http://localhost:8080/api/games", body: { id: "0b7c2a8e-4f1d-4c3a-9e5b-1d2f3a4b5c6d" } },
    ]);
    const { result } = renderHook(() => useCreateGame(), { wrapper: clientWrapper() });

    act(() => result.current.createGame("TRIPOINT", 5, true));

    await waitFor(() => expect(router.push).toHaveBeenCalledWith("/games/0b7c2a8e-4f1d-4c3a-9e5b-1d2f3a4b5c6d"));
    const [call] = callsTo(fetchMock);
    expect(call.method).toBe("POST");
    expect(call.body).toEqual({ gameType: "TRIPOINT", numberOfRounds: 5, hardMode: true });
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

    act(() => result.current.createGame("RANDOM", 3, false));
    await waitFor(() => expect(result.current.isLoading).toBe(true));

    await act(async () =>
      finish({ ok: true, status: 200, json: async () => ({ id: "0b7c2a8e-4f1d-4c3a-9e5b-1d2f3a4b5c6d" }) }),
    );
    await waitFor(() => expect(result.current.isLoading).toBe(false));
  });

  it("reports an error and does not navigate when the server refuses", async () => {
    router.push.mockClear();
    mockFetch([{ method: "POST", url: /\/api\/games$/, status: 500, body: {} }]);
    const { result } = renderHook(() => useCreateGame(), { wrapper: clientWrapper() });

    act(() => result.current.createGame("TRIPOINT", 5, false));

    await waitFor(() => expect(result.current.error).toBeTruthy());
    expect(String(result.current.error)).toContain("Failed to create game (500)");
    expect(router.push).not.toHaveBeenCalled();
  });
});
