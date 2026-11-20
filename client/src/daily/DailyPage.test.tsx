import { act, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { localDateString } from "../api/daily";
import { callsTo, mockFetch } from "../test/fetchMock";
import { makeDailyGameResponse, makeDailyScore, makeGameRounds } from "../test/fixtures";
import { renderWithClient } from "../test/render";
import DailyPage from "./DailyPage";
import { DailyEntry, readDaily, resetDailyStorageForTests, writeDaily } from "./dailyStorage";

vi.mock("../games/common/GameRoundPage", () => ({
  default: ({ gameId, daily }: { gameId: string; daily: { date: string } }) => (
    <div>
      game {gameId} (daily {daily.date})
    </div>
  ),
}));

const API = "http://localhost:8080";
const ROUND_0 = API + "/api/games/g1/rounds/0";
const SCORE = {
  url: API + `/api/daily-scores/${localDateString()}?difficulty=NORMAL`,
  body: makeDailyScore({ date: localDateString() }),
};
const today = localDateString();
const entry: DailyEntry = { v: 1, date: today, number: 2, gameId: "g1", difficulty: "NORMAL" };

const unfinished = makeGameRounds([{ answerState: "CORRECT" }, { answerState: "UNANSWERED" }]);
const finishedGame = makeGameRounds([{ answerState: "CORRECT" }, { river: true, answerState: "GAVE_UP" }]);

beforeEach(resetDailyStorageForTests);

function posts(fetchMock: ReturnType<typeof mockFetch>) {
  return callsTo(fetchMock).filter((c) => c.method === "POST");
}

describe("DailyPage intro", () => {
  it("says what each round shows", async () => {
    mockFetch([{ url: API + "/health", body: "ok" }]);
    renderWithClient(<DailyPage />);

    expect(await screen.findByText(/Five geography puzzles from a variety of game modes/)).toHaveTextContent(
      "Wrong answers don't lower your score, but hints do.",
    );
  });
});

describe("DailyPage with no game for today", () => {
  it("offers to play and creates nothing until it is clicked", async () => {
    const fetchMock = mockFetch([
      { url: API + "/health", body: "ok" },
      { method: "POST", url: API + "/api/daily-games", body: makeDailyGameResponse({ id: "g1", number: 7 }) },
    ]);
    renderWithClient(<DailyPage />);

    const play = await screen.findByRole("button", { name: "Play today's challenge" });
    expect(posts(fetchMock)).toHaveLength(0);

    await userEvent.click(play);

    await waitFor(() =>
      expect(readDaily()).toEqual({ v: 1, date: today, number: 7, gameId: "g1", difficulty: "NORMAL" }),
    );
    expect(posts(fetchMock).map((c) => c.body)).toEqual([{ date: today, difficulty: "NORMAL" }]);
  });

  it.each([
    ["easy", "EASY"],
    ["hard", "HARD"],
  ])("records the %s difficulty the game started with", async (stored, difficulty) => {
    localStorage.setItem("gg-difficulty", stored);
    const fetchMock = mockFetch([
      { url: API + "/health", body: "ok" },
      { method: "POST", url: API + "/api/daily-games", body: makeDailyGameResponse({ id: "g1" }) },
    ]);
    renderWithClient(<DailyPage />);

    await userEvent.click(await screen.findByRole("button", { name: "Play today's challenge" }));

    await waitFor(() => expect(readDaily()?.difficulty).toBe(difficulty));
    expect(posts(fetchMock).map((c) => c.body)).toEqual([{ date: today, difficulty }]);
  });

  it("says when the server can't be reached", async () => {
    mockFetch([
      { url: API + "/health", body: "ok" },
      { method: "POST", url: API + "/api/daily-games", status: 500 },
    ]);
    renderWithClient(<DailyPage />);

    await userEvent.click(await screen.findByRole("button", { name: "Play today's challenge" }));

    expect(await screen.findByText(/Couldn't start/)).toBeInTheDocument();
    expect(readDaily()).toBeNull();
  });

  it("uses a game another tab started in the meantime", async () => {
    const fetchMock = mockFetch([{ url: API + "/health", body: "ok" }]);
    renderWithClient(<DailyPage />);
    const play = await screen.findByRole("button", { name: "Play today's challenge" });
    // Written straight to storage: this tab hasn't heard about it.
    localStorage.setItem("gg-daily", JSON.stringify(entry));

    await userEvent.click(play);

    expect(posts(fetchMock)).toHaveLength(0);
  });

  it("forgets an entry from another day", async () => {
    writeDaily({ ...entry, date: "2020-01-01" });
    mockFetch([{ url: API + "/health", body: "ok" }]);
    renderWithClient(<DailyPage />);

    expect(await screen.findByRole("button", { name: "Play today's challenge" })).toBeInTheDocument();
  });

  it("shows a waking-up note when starting is slow", async () => {
    vi.useFakeTimers();
    try {
      mockFetch([
        { url: API + "/health", body: "ok" },
        { method: "POST", url: API + "/api/daily-games", body: () => new Promise(() => {}) },
      ]);
      // A body function that never resolves isn't supported by the router, so stub fetch for the POST.
      const base = globalThis.fetch;
      vi.stubGlobal("fetch", (input: RequestInfo | URL, init?: RequestInit) =>
        init?.method === "POST" ? new Promise(() => {}) : base(input, init),
      );
      renderWithClient(<DailyPage />);
      const { fireEvent } = await import("@testing-library/react");

      fireEvent.click(screen.getByRole("button", { name: "Play today's challenge" }));
      await act(async () => {
        await vi.advanceTimersByTimeAsync(3100);
      });

      expect(screen.getByText("Waking up the server…")).toBeInTheDocument();
    } finally {
      vi.useRealTimers();
    }
  });
});

describe("DailyPage with a game for today", () => {
  it("shows the game of an unfinished entry", async () => {
    writeDaily(entry);
    mockFetch([{ url: ROUND_0, body: unfinished[0] }]);
    renderWithClient(<DailyPage />);

    expect(await screen.findByText(`game g1 (daily ${today})`)).toBeInTheDocument();
    expect(readDaily()?.result).toBeUndefined();
  });

  it("clears the entry when the game is gone, so the daily can be played again", async () => {
    writeDaily(entry);
    mockFetch([
      { url: API + "/health", body: "ok" },
      { url: ROUND_0, status: 404 },
    ]);
    renderWithClient(<DailyPage />);

    expect(await screen.findByRole("button", { name: "Play today's challenge" })).toBeInTheDocument();
    expect(readDaily()).toBeNull();
  });

  it("writes the result when the last round is finished, and stays on the game", async () => {
    writeDaily(entry);
    mockFetch([{ url: ROUND_0, body: finishedGame[0] }]);
    renderWithClient(<DailyPage />);

    await waitFor(() =>
      expect(readDaily()?.result).toEqual({ score: 100, maxScore: 200, rounds: ["correct", "missed"] }),
    );
    expect(screen.getByText(`game g1 (daily ${today})`)).toBeInTheDocument();
  });

  it("counts a round with hints as 'hints'", async () => {
    writeDaily(entry);
    const rounds = makeGameRounds([{ answerState: "CORRECT", hintsUsed: 1 }, { answerState: "CORRECT" }]);
    mockFetch([{ url: ROUND_0, body: rounds[0] }]);
    renderWithClient(<DailyPage />);

    await waitFor(() => expect(readDaily()?.result?.rounds).toEqual(["hints", "correct"]));
  });

  it("stores the server's difficulty with the result, and the finished page shares it", async () => {
    writeDaily(entry);
    mockFetch([{ url: ROUND_0, body: { ...finishedGame[0], difficulty: "HARD", hardMode: true } }, SCORE]);
    const { unmount } = renderWithClient(<DailyPage />);

    await waitFor(() => expect(readDaily()?.difficulty).toBe("HARD"));
    expect(readDaily()?.result).toBeDefined();
    unmount();

    renderWithClient(<DailyPage />);
    expect(await screen.findByLabelText("Your result")).toHaveTextContent(`GeoGauntlet Daily ${today} · Hard mode`);
  });

  it("does not take the difficulty from the preference alone", async () => {
    localStorage.setItem("gg-difficulty", "hard");
    writeDaily(entry);
    mockFetch([{ url: ROUND_0, body: finishedGame[0] }]);
    renderWithClient(<DailyPage />);

    await waitFor(() => expect(readDaily()?.result).toBeDefined());
    expect(readDaily()?.difficulty).toBe("NORMAL");
  });

  it("shows a finished entry's result without fetching the game", async () => {
    const result = {
      score: 430,
      maxScore: 500,
      rounds: ["firstGuess", "hints", "correct", "missed", "firstGuess"] as const,
    };
    writeDaily({ ...entry, number: 42, difficulty: "HARD", result: { ...result, rounds: [...result.rounds] } });
    const hardScore = { ...SCORE, url: SCORE.url.replace("NORMAL", "HARD") };
    const fetchMock = mockFetch([hardScore]);
    renderWithClient(<DailyPage />);

    expect(await screen.findByText("You scored 430 / 500")).toBeInTheDocument();
    expect(screen.getByLabelText("Your result")).toHaveTextContent(`GeoGauntlet Daily ${today} · Hard mode`);
    expect(screen.getByLabelText("Your result")).toHaveTextContent("⭐🟨🟩🟥⭐");
    expect(screen.getByRole("button", { name: "Copy result" })).toBeInTheDocument();
    // Only the optional comparison is fetched, from the game's own league, never the game.
    expect(callsTo(fetchMock).map((c) => c.url)).toEqual([hardScore.url]);
  });

  it("opens the rounds of a finished entry", async () => {
    writeDaily({ ...entry, result: { score: 100, maxScore: 200, rounds: ["firstGuess", "missed"] } });
    mockFetch([{ url: ROUND_0, body: finishedGame[0] }, SCORE]);
    renderWithClient(<DailyPage />);

    await userEvent.click(await screen.findByRole("button", { name: "See the rounds" }));

    expect(await screen.findByText(`game g1 (daily ${today})`)).toBeInTheDocument();
  });

  it("keeps a finished entry's result even if the game has been deleted", async () => {
    writeDaily({ ...entry, result: { score: 100, maxScore: 200, rounds: ["firstGuess", "missed"] } });
    mockFetch([{ url: ROUND_0, status: 404 }, SCORE]);
    renderWithClient(<DailyPage />);

    await userEvent.click(await screen.findByRole("button", { name: "See the rounds" }));
    await screen.findByText(`game g1 (daily ${today})`);

    expect(readDaily()?.result).toBeDefined();
  });
});

describe("DailyPage finished view", () => {
  it("shows the day's average", async () => {
    writeDaily({ ...entry, result: { score: 100, maxScore: 200, rounds: ["firstGuess", "missed"] } });
    mockFetch([SCORE]);
    renderWithClient(<DailyPage />);

    expect(await screen.findByText(/Today.s average: 312\.5/)).toBeInTheDocument();
  });
});
