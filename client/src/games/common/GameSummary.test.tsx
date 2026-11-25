import { screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { callsTo, mockFetch, Route } from "../../test/fetchMock";
import { makeDailyScore, makeGameRounds } from "../../test/fixtures";
import { renderWithClient } from "../../test/render";
import { GameRoundResponse } from "../../api/types";
import GameSummary from "./GameSummary";

const router = vi.hoisted(() => ({ push: vi.fn(), query: {} }));
vi.mock("next/router", () => ({ useRouter: () => router }));

const roundsUrl = /\/api\/games\/g1\/rounds\/(\d+)$/;

function roundsRoute(rounds: GameRoundResponse[]): Route {
  return { url: roundsUrl, body: ({ url }) => rounds[Number(url.match(roundsUrl)![1])] };
}

// A first-guess tripoint, a revealed river and a tripoint answered with one of its three hints.
const mixed = makeGameRounds([
  { answerState: "CORRECT", firstGuess: true },
  { river: true, answerState: "GAVE_UP" },
  { answerState: "CORRECT", hintsUsed: 1 },
]);

const hard = (rounds: GameRoundResponse[]) => rounds.map((r) => ({ ...r, difficulty: "HARD" as const }));
const easy = (rounds: GameRoundResponse[]) => rounds.map((r) => ({ ...r, difficulty: "EASY" as const }));

afterEach(() => {
  localStorage.clear();
  Object.defineProperty(navigator, "clipboard", { value: undefined, configurable: true });
});

function renderSummary(onOpenRound = vi.fn(), totalRoundCount = 3) {
  const view = renderWithClient(
    <GameSummary gameId="g1" totalRoundCount={totalRoundCount} onOpenRound={onOpenRound} />,
  );
  return { ...view, onOpenRound };
}

describe("GameSummary for a daily challenge", () => {
  const daily = { date: "2026-11-18" };
  const done = makeGameRounds([
    { answerState: "CORRECT", firstGuess: true },
    { river: true, answerState: "GAVE_UP" },
  ]);
  const scoresRoute: Route = {
    url: "http://localhost:8080/api/daily-scores/2026-11-18?difficulty=NORMAL",
    body: makeDailyScore(),
  };

  function renderDaily() {
    return renderWithClient(<GameSummary gameId="g1" totalRoundCount={2} onOpenRound={vi.fn()} daily={daily} />);
  }

  it("offers Copy result and a Mixed game instead of Play again", async () => {
    mockFetch([roundsRoute(done), scoresRoute]);
    renderDaily();

    expect(await screen.findByRole("button", { name: "Copy result" })).toBeInTheDocument();
    expect(screen.getByText(/Daily 2026-11-18/)).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Play again" })).not.toBeInTheDocument();
  });

  it("starts a five round Mixed game", async () => {
    const fetchMock = mockFetch([
      roundsRoute(done),
      scoresRoute,
      { method: "POST", url: "http://localhost:8080/api/games", body: { id: "new" } },
    ]);
    renderDaily();

    await userEvent.click(await screen.findByRole("button", { name: "Play a Mixed game" }));

    await waitFor(() =>
      expect(
        callsTo(fetchMock)
          .filter((c) => c.method === "POST")
          .map((c) => c.body),
      ).toEqual([{ gameType: "RANDOM", numberOfRounds: 5, difficulty: "NORMAL" }]),
    );
  });

  it("starts the Mixed game in Hard mode after a hard daily, whatever the preference", async () => {
    const fetchMock = mockFetch([
      roundsRoute(hard(done)),
      scoresRoute,
      { method: "POST", url: "http://localhost:8080/api/games", body: { id: "new" } },
    ]);
    renderDaily();

    await userEvent.click(await screen.findByRole("button", { name: "Play a Mixed game" }));

    await waitFor(() =>
      expect(callsTo(fetchMock).find((c) => c.method === "POST")!.body).toEqual({
        gameType: "RANDOM",
        numberOfRounds: 5,
        difficulty: "HARD",
      }),
    );
  });

  async function copiedText(rounds: GameRoundResponse[]) {
    mockFetch([roundsRoute(rounds), scoresRoute]);
    renderDaily();
    await userEvent.click(await screen.findByRole("button", { name: "Copy result" }));
    return (await screen.findByRole("textbox", { name: "Copy this text" })) as HTMLTextAreaElement;
  }

  it("puts Hard mode in the copied title of a hard daily, whatever the preference says", async () => {
    expect((await copiedText(hard(done))).value).toContain("Daily 2026-11-18 · Hard mode");
  });

  it("puts Easy in the copied title of an easy daily", async () => {
    expect((await copiedText(easy(done))).value).toContain("Daily 2026-11-18 · Easy");
  });

  it("leaves the difficulty out of a normal daily's copied text, even with the preference on", async () => {
    localStorage.setItem("gg-difficulty", "hard");
    expect((await copiedText(done)).value).not.toContain("Hard mode");
  });

  it("compares an easy daily with the Easy league", async () => {
    mockFetch([
      roundsRoute(easy(done)),
      {
        url: "http://localhost:8080/api/daily-scores/2026-11-18?difficulty=EASY",
        body: makeDailyScore({ difficulty: "EASY", players: 4, averageScore: 300 }),
      },
    ]);
    renderDaily();

    expect(await screen.findByText(/Today's average \(Easy players\)/)).toBeInTheDocument();
  });

  it("adds Hard mode to the meta line of a hard daily", async () => {
    mockFetch([roundsRoute(hard(done)), scoresRoute]);
    renderDaily();

    expect(await screen.findByText("Daily 2026-11-18 · Hard mode")).toBeInTheDocument();
  });

  it("has no Copy result button until the game is finished", async () => {
    mockFetch([roundsRoute(makeGameRounds([{ answerState: "CORRECT" }, { answerState: "UNANSWERED" }])), scoresRoute]);
    renderDaily();

    expect(await screen.findByRole("heading", { name: "Your result" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Copy result" })).not.toBeInTheDocument();
  });

  it("shows the day's average from three players", async () => {
    mockFetch([roundsRoute(done), scoresRoute]);
    renderDaily();

    expect(await screen.findByText("Today's average: 312.5 / 500")).toBeInTheDocument();
  });

  it("makes no daily-scores request for a game that is not the daily", async () => {
    const fetchMock = mockFetch([roundsRoute(mixed), scoresRoute]);
    renderSummary();

    await screen.findByRole("heading", { name: "Your result" });
    expect(callsTo(fetchMock).filter((c) => c.url.includes("daily-scores"))).toEqual([]);
  });
});

describe("GameSummary", () => {
  it("shows a spinner while the rounds load", () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(() => new Promise(() => {})),
    );
    renderSummary();

    expect(screen.getByRole("status")).toBeTruthy();
    expect(screen.queryByRole("heading", { name: "Your result" })).not.toBeInTheDocument();
  });

  it("has no Copy result button and offers Play again in an ordinary game", async () => {
    mockFetch([roundsRoute(mixed)]);
    renderSummary();

    expect(await screen.findByRole("button", { name: "Play again" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Copy result" })).not.toBeInTheDocument();
  });

  it("shows the score and one row per round", async () => {
    mockFetch([roundsRoute(mixed)]);
    renderSummary();

    expect(await screen.findByText("19")).toBeInTheDocument();

    const rows = screen.getAllByRole("button").filter((b) => /^\d\./.test(b.textContent ?? ""));
    expect(rows).toHaveLength(3);
    expect(within(rows[0]).getByText("Tripoints")).toBeInTheDocument();
    expect(rows[0]).toHaveTextContent("Belgium · Netherlands · Germany");
    expect(rows[0]).toHaveTextContent("+10");
    expect(rows[0]).not.toHaveTextContent("hint");
    expect(within(rows[1]).getByText("River Shapes")).toBeInTheDocument();
    expect(rows[1]).toHaveTextContent("Danube");
    expect(rows[1]).toHaveTextContent("+0");
    expect(rows[2]).toHaveTextContent("+9");
    expect(rows[2]).toHaveTextContent("1 hint");
  });

  it("adds Hard mode to the meta line of a hard game, and does not mention it otherwise", async () => {
    mockFetch([roundsRoute(hard(mixed))]);
    const { unmount } = renderSummary();
    expect(await screen.findByText("Mixed · Hard mode")).toBeInTheDocument();
    unmount();

    mockFetch([roundsRoute(mixed)]);
    renderSummary();
    expect(await screen.findByText("Mixed")).toBeInTheDocument();
    expect(screen.queryByText(/Hard mode/)).not.toBeInTheDocument();
  });

  it("adds Easy to the meta line of an easy game", async () => {
    mockFetch([roundsRoute(easy(mixed))]);
    renderSummary();

    expect(await screen.findByText("Mixed · Easy")).toBeInTheDocument();
  });

  it("names the game type before the difficulty when every round is one type", async () => {
    mockFetch([roundsRoute(hard(makeGameRounds([{ answerState: "CORRECT" }, { answerState: "CORRECT" }])))]);
    renderSummary(vi.fn(), 2);

    expect(await screen.findByText(/^.+ · Hard mode$/)).toHaveClass("gg-summary__meta");
  });

  it("shows only Perfect rounds and Hints used in the stats", async () => {
    mockFetch([roundsRoute(mixed)]);
    renderSummary();
    await screen.findByRole("heading", { name: "Your result" });

    expect(screen.getAllByRole("listitem").map((li) => li.textContent)).toEqual([
      "Perfect rounds 1 of 3",
      "Hints used 1",
    ]);
    expect(screen.queryByText(/first guess/)).not.toBeInTheDocument();
  });

  it("plays an easy game again as Easy", async () => {
    const fetchMock = mockFetch([
      roundsRoute(easy(mixed)),
      { method: "POST", url: /\/api\/games$/, body: { id: "0b7c2a8e-4f1d-4c3a-9e5b-1d2f3a4b5c6d" } },
    ]);
    renderSummary();
    await screen.findByText("19");

    await userEvent.setup().click(screen.getByRole("button", { name: "Play again" }));

    await waitFor(() => expect(callsTo(fetchMock).some((c) => c.method === "POST")).toBe(true));
    expect(callsTo(fetchMock).find((c) => c.method === "POST")!.body).toMatchObject({ difficulty: "EASY" });
  });

  it("shows Solved 0 with Hard mode for a hard game with no answered rounds", async () => {
    mockFetch([roundsRoute(hard(makeGameRounds([{ answerState: "UNANSWERED" }])))]);
    renderSummary(vi.fn(), 1);

    expect(await screen.findByText(/· Hard mode$/)).toBeInTheDocument();
    expect(screen.getByText("0 of 1")).toBeInTheDocument();
  });

  it("plays again with the game's difficulty, not the preference", async () => {
    localStorage.setItem("gg-difficulty", "hard");
    const fetchMock = mockFetch([
      roundsRoute(mixed),
      { method: "POST", url: /\/api\/games$/, body: { id: "0b7c2a8e-4f1d-4c3a-9e5b-1d2f3a4b5c6d" } },
    ]);
    renderSummary();
    await screen.findByText("19");

    await userEvent.setup().click(screen.getByRole("button", { name: "Play again" }));

    await waitFor(() => expect(callsTo(fetchMock).some((c) => c.method === "POST")).toBe(true));
    expect(callsTo(fetchMock).find((c) => c.method === "POST")!.body).toEqual({
      gameType: "RANDOM",
      numberOfRounds: 3,
      difficulty: "NORMAL",
    });
  });

  it("shows the site footer with the credits", async () => {
    mockFetch([roundsRoute(mixed)]);
    renderSummary();

    await screen.findByText("19");
    expect(screen.getByRole("contentinfo")).toBeInTheDocument();
  });

  it("falls back to the raw game type name, with no answers, for a type it has no module for", async () => {
    const unknown = makeGameRounds([{ answerState: "CORRECT" }]).map((round) => ({
      ...round,
      gameType: { ...round.gameType, name: "MYSTERY_GAME" },
    }));
    mockFetch([roundsRoute(unknown)]);
    renderSummary(vi.fn(), 1);

    const row = (await screen.findAllByRole("button")).find((b) => /^1\./.test(b.textContent ?? ""))!;

    expect(screen.getByText("MYSTERY_GAME")).toBeInTheDocument();
    expect(row).not.toHaveTextContent("Belgium");
  });

  it("names the game type once at the top, not on each row, when every round shared it", async () => {
    const same = makeGameRounds([
      { answerState: "CORRECT", firstGuess: true },
      { answerState: "CORRECT", firstGuess: true },
    ]).map((round) => ({
      ...round,
      gameType: { ...round.gameType, name: "MYSTERY_GAME" },
    }));
    mockFetch([roundsRoute(same)]);
    renderSummary(vi.fn(), 2);

    const rows = (await screen.findAllByRole("button")).filter((b) => /^\d\./.test(b.textContent ?? ""));

    expect(screen.getAllByText("MYSTERY_GAME")).toHaveLength(1);
    expect(rows).toHaveLength(2);
    expect(rows[0]).not.toHaveTextContent("MYSTERY_GAME");
  });

  it("counts a round that ran out of guesses as missed and says so", async () => {
    mockFetch([
      roundsRoute(
        makeGameRounds([
          { answerState: "CORRECT", firstGuess: true },
          { river: true, answerState: "OUT_OF_GUESSES" },
        ]),
      ),
    ]);
    renderSummary(vi.fn(), 2);

    expect(await screen.findByText("10")).toBeInTheDocument();
    expect(screen.getByText("Ran out of lives")).toBeInTheDocument();
  });

  it("celebrates a perfect game", async () => {
    mockFetch([
      roundsRoute(
        makeGameRounds([
          { answerState: "CORRECT", firstGuess: true },
          { answerState: "CORRECT", firstGuess: true },
        ]),
      ),
    ]);
    renderSummary(vi.fn(), 2);

    expect(await screen.findByRole("heading", { name: "Perfect game!" })).toBeInTheDocument();
    expect(screen.getByText("Perfect rounds")).toBeInTheDocument();
    expect(screen.getByText("2 of 2")).toBeInTheDocument();
  });

  it("is not a perfect game when a wrong guess cost a point in one round", async () => {
    mockFetch([
      roundsRoute(makeGameRounds([{ answerState: "CORRECT" }, { answerState: "CORRECT", wrongGuesses: ["Spain"] }])),
    ]);
    renderSummary(vi.fn(), 2);

    expect(await screen.findByRole("heading", { name: "Your result" })).toBeInTheDocument();
    expect(screen.queryByText(/Perfect game/)).not.toBeInTheDocument();
    expect(screen.getByText("1 of 2")).toBeInTheDocument();
  });

  it("counts the perfect rounds of a mix of outcomes", async () => {
    mockFetch([
      roundsRoute(
        makeGameRounds([
          { answerState: "CORRECT", firstGuess: true },
          { answerState: "CORRECT" },
          { answerState: "CORRECT", hintsUsed: 1 },
          { answerState: "GAVE_UP", answersFound: 2 },
          { river: true, answerState: "OUT_OF_GUESSES" },
        ]),
      ),
    ]);
    renderSummary(vi.fn(), 5);

    expect(await screen.findByText("Perfect rounds")).toBeInTheDocument();
    expect(screen.getByText("2 of 5")).toBeInTheDocument();
    expect(screen.getByText("34")).toBeInTheDocument();
  });

  it("counts the perfect rounds and the hints of a mixed result", async () => {
    mockFetch([roundsRoute(makeGameRounds([{ answerState: "CORRECT", hintsUsed: 3 }, { answerState: "GAVE_UP" }]))]);
    renderSummary(vi.fn(), 2);

    expect(await screen.findByText("7")).toBeInTheDocument();
    expect(screen.getByText("0 of 2")).toBeInTheDocument();
    expect(screen.getByText(/3 hints/)).toBeInTheDocument();
  });

  it("draws a score bar for each round, with its parts as text", async () => {
    mockFetch([roundsRoute(makeGameRounds([{ answerState: "CORRECT", hintsUsed: 1 }, { answerState: "GAVE_UP" }]))]);
    renderSummary(vi.fn(), 2);

    expect(
      await screen.findByRole("img", { name: "9 of 10: answer 4, no hints 2 of 3, no misses 3 of 3" }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("img", { name: "0 of 10: answer 0, no hints 0 of 3, no misses 0 of 3" }),
    ).toBeInTheDocument();
  });

  it("opens the round that was clicked", async () => {
    mockFetch([roundsRoute(mixed)]);
    const { onOpenRound } = renderSummary();
    await screen.findByText("19");

    await userEvent.setup().click(screen.getByRole("button", { name: /River Shapes/ }));

    expect(onOpenRound).toHaveBeenCalledWith(1);
  });

  it("links home from the header and from the button", async () => {
    mockFetch([roundsRoute(mixed)]);
    renderSummary();
    await screen.findByText("19");

    const links = screen.getAllByRole("link", { name: "Home" });
    expect(links).toHaveLength(2);
    links.forEach((link) => expect(link).toHaveAttribute("href", "/"));
  });

  it("plays again with a random mix when the game had different types", async () => {
    const fetchMock = mockFetch([
      roundsRoute(mixed),
      { method: "POST", url: /\/api\/games$/, body: { id: "0b7c2a8e-4f1d-4c3a-9e5b-1d2f3a4b5c6d" } },
    ]);
    renderSummary();
    await screen.findByText("19");

    await userEvent.setup().click(screen.getByRole("button", { name: "Play again" }));

    await waitFor(() => expect(router.push).toHaveBeenCalledWith("/games/0b7c2a8e-4f1d-4c3a-9e5b-1d2f3a4b5c6d"));
    const post = callsTo(fetchMock).find((c) => c.method === "POST")!;
    expect(post.body).toEqual({ gameType: "RANDOM", numberOfRounds: 3, difficulty: "NORMAL" });
  });

  it("plays again with the same type when every round shared it", async () => {
    const rivers = makeGameRounds([
      { river: true, answerState: "CORRECT", firstGuess: true },
      { river: true, answerState: "CORRECT", firstGuess: true },
    ]);
    const fetchMock = mockFetch([
      roundsRoute(rivers),
      { method: "POST", url: /\/api\/games$/, body: { id: "0b7c2a8e-4f1d-4c3a-9e5b-1d2f3a4b5c6d" } },
    ]);
    renderSummary(vi.fn(), 2);
    await screen.findByRole("heading", { name: "Perfect game!" });

    await userEvent.setup().click(screen.getByRole("button", { name: "Play again" }));

    await waitFor(() => expect(callsTo(fetchMock).some((c) => c.method === "POST")).toBe(true));
    expect(callsTo(fetchMock).find((c) => c.method === "POST")!.body).toEqual({
      gameType: "RIVERS_BY_SHAPE",
      numberOfRounds: 2,
      difficulty: "NORMAL",
    });
  });

  it("tells the player when a new game could not be started", async () => {
    mockFetch([roundsRoute(mixed), { method: "POST", url: /\/api\/games$/, status: 500, body: {} }]);
    renderSummary();
    await screen.findByText("19");

    await userEvent.setup().click(screen.getByRole("button", { name: "Play again" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Couldn't start a new game. Try again.");
  });

  it("says when too many games were started from the network", async () => {
    mockFetch([roundsRoute(mixed), { method: "POST", url: /\/api\/games$/, status: 429, body: {} }]);
    renderSummary();
    await screen.findByRole("heading", { name: "Your result" });

    await userEvent.setup().click(screen.getByRole("button", { name: "Play again" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Too many new games from your network");
  });
});

describe("GameSummary with partly correct rounds", () => {
  const partly = makeGameRounds([
    { answerState: "CORRECT", firstGuess: true },
    { answerState: "GAVE_UP", answersFound: 2 },
    { river: true, answerState: "GAVE_UP" },
  ]);

  it("counts a revealed round with points as with hints, apart from the missed ones", async () => {
    mockFetch([roundsRoute(partly)]);
    renderSummary();

    expect(await screen.findByText("15")).toBeInTheDocument();
  });

  it("shows the partly correct round as a yellow gain, not as missed", async () => {
    mockFetch([roundsRoute(partly)]);
    renderSummary();

    const row = await screen.findByText("+5");
    expect(row.closest("[data-outcome]")).toHaveAttribute("data-outcome", "partial");
  });
});

describe("GameSummary stats and table", () => {
  it("shows the perfect round and hint totals and each round's wrong guesses", async () => {
    mockFetch([
      roundsRoute(
        makeGameRounds([
          { answerState: "CORRECT", firstGuess: true },
          { river: true, answerState: "GAVE_UP", wrongGuesses: ["Rhine", "Elbe"] },
          { answerState: "CORRECT", hintsUsed: 1 },
        ]),
      ),
    ]);
    renderSummary();
    await screen.findByRole("heading", { name: "Your result" });

    const stats = screen.getAllByRole("listitem").map((li) => li.textContent);
    expect(stats).toEqual(["Perfect rounds 1 of 3", "Hints used 1"]);
    const rows = screen.getAllByRole("button").filter((b) => /^\d\./.test(b.textContent ?? ""));
    expect(rows[1]).toHaveTextContent("Rhine, Elbe");
    expect(rows[1]).toHaveTextContent("Gave up");
    expect(rows[0]).toHaveTextContent("–");
  });

  it("marks each answer a revealed round didn't find", async () => {
    const rounds = makeGameRounds([{ answerState: "GAVE_UP" }, { answerState: "CORRECT" }]);
    rounds[0] = { ...rounds[0], guesses: [{ text: "Holland", correct: true }] };
    mockFetch([roundsRoute(rounds)]);
    renderSummary(vi.fn(), 2);
    await screen.findByRole("heading", { name: "Your result" });

    const row = screen.getAllByRole("button").find((b) => /^1\./.test(b.textContent ?? ""))!;
    const missed = row.querySelectorAll('[data-outcome="missed"]');
    expect([...missed].map((el) => el.textContent)).toEqual(["Belgium (not found)", "Germany (not found)"]);
  });
});
