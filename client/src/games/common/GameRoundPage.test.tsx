import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { callsTo, mockFetch } from "../../test/fetchMock";
import { countryNames, riverNames, makeGameRounds, makeRound } from "../../test/fixtures";
import { AnswerState } from "./Model";
import GameRoundPage from "./GameRoundPage";

vi.mock("next/head", () => ({ default: ({ children }: { children: React.ReactNode }) => <>{children}</> }));
vi.mock("next/router", () => ({ useRouter: () => ({ push: vi.fn(), query: {} }) }));
vi.mock("../tripoint/TripointMapWrapper", () => ({ default: () => <div data-testid="map" /> }));
vi.mock("../rivers/RiverShapesMapWrapper", () => ({ default: () => <div data-testid="map" /> }));

const TRIPOINT_PROMPT = "Name the 3 countries that meet at the flag.";
const RIVER_PROMPT = "Name this river from its shape.";

let client: QueryClient;

beforeEach(() => {
  client = new QueryClient({ defaultOptions: { queries: { retry: false, refetchOnWindowFocus: false } } });
});

afterEach(() => {
  client.clear();
});

type Kind = "TRIPOINT" | "RIVERS_BY_SHAPE";

/** A tiny in-memory game server that keeps round states and hints and recomputes the game result. */
function fakeServer(kinds: Kind[], initialStates: AnswerState[] = kinds.map((): AnswerState => "UNANSWERED")) {
  const states = [...initialStates];
  const hints = kinds.map(() => 0);
  const build = (i: number) =>
    makeGameRounds(
      kinds.map((kind, n) => ({ river: kind === "RIVERS_BY_SHAPE", answerState: states[n], hintsUsed: hints[n] })),
    )[i];
  return mockFetch([
    { url: /\/api\/games\/g1\/rounds\/\d+$/, body: ({ url }) => build(Number(url.split("/").pop())) },
    {
      method: "POST",
      url: /\/api\/games\/g1\/rounds\/\d+\/answers$/,
      body: ({ url, init }) => {
        const index = Number(url.match(/rounds\/(\d+)\/answers/)![1]);
        const answer = JSON.parse(init!.body as string);
        states[index] = answer.answerState;
        hints[index] = answer.hintsUsed;
        return build(index);
      },
    },
    { url: /\/api\/countries$/, body: countryNames },
    { url: /\/api\/rivers$/, body: riverNames },
  ]);
}

function renderPage() {
  return render(
    <QueryClientProvider client={client}>
      <GameRoundPage gameId="g1" />
    </QueryClientProvider>,
  );
}

// React 19 hoists <title> into <head>.
const title = () => document.title;
const roundGets = (fetchMock: ReturnType<typeof mockFetch>, round: number) =>
  callsTo(fetchMock).filter((c) => c.method === "GET" && c.url.endsWith(`/rounds/${round}`));

async function solveTripoint(user: ReturnType<typeof userEvent.setup>) {
  const input = screen.getByLabelText("Country name…");
  for (const name of ["Belgium", "Netherlands", "Germany"]) {
    await user.type(input, name + "{Enter}");
  }
}

describe("GameRoundPage loading and errors", () => {
  it("shows a spinner and a loading title while the round loads", () => {
    vi.stubGlobal("fetch", vi.fn(() => new Promise(() => {})));
    const { container } = renderPage();

    expect(container.querySelector(".spinner-border")).not.toBeNull();
    expect(title()).toBe("Loading... | Geogames");
  });

  it("shows an error when the round cannot be loaded", async () => {
    mockFetch([{ url: /\/rounds\/0$/, status: 500, body: {} }]);
    renderPage();

    expect(await screen.findByRole("alert")).toHaveTextContent("Couldn't load this round. Please refresh.");
  });
});

describe("GameRoundPage game types", () => {
  it("renders the tripoint game and title", async () => {
    fakeServer(["TRIPOINT"]);
    renderPage();

    expect(await screen.findByText(TRIPOINT_PROMPT)).toBeInTheDocument();
    expect(title()).toBe("Tripoints | Geogames");
  });

  it("renders the river game and title", async () => {
    fakeServer(["RIVERS_BY_SHAPE"]);
    renderPage();

    expect(await screen.findByText(RIVER_PROMPT)).toBeInTheDocument();
    expect(title()).toBe("River Shapes | Geogames");
  });

  it("copes with a game type it does not know", async () => {
    mockFetch([
      {
        url: /\/rounds\/0$/,
        body: makeRound({ gameType: { name: "MYSTERY", answerCount: 1 } }),
      },
    ]);
    renderPage();

    expect(await screen.findByText("Unknown game type...")).toBeInTheDocument();
    expect(title()).toBe("Unknown game! | Geogames");
  });
});

describe("GameRoundPage playing", () => {
  it("posts the answer and then reloads the round", async () => {
    const fetchMock = fakeServer(["TRIPOINT", "TRIPOINT"]);
    const user = userEvent.setup();
    renderPage();
    await screen.findByText(TRIPOINT_PROMPT);
    expect(roundGets(fetchMock, 0)).toHaveLength(1);

    await solveTripoint(user);

    await waitFor(() => {
      const posts = callsTo(fetchMock).filter((c) => c.method === "POST");
      expect(posts).toHaveLength(1);
      expect(posts[0].url).toBe("http://localhost:8080/api/games/g1/rounds/0/answers");
      expect(posts[0].body).toEqual({ answerState: "CORRECT", hintsUsed: 0, wrongGuesses: 0 });
    });
    await waitFor(() => expect(roundGets(fetchMock, 0).length).toBeGreaterThanOrEqual(2));
  });

  it("moves to the next round", async () => {
    const fetchMock = fakeServer(["TRIPOINT", "RIVERS_BY_SHAPE"]);
    const user = userEvent.setup();
    renderPage();
    await screen.findByText(TRIPOINT_PROMPT);

    await solveTripoint(user);
    await user.click(await screen.findByRole("button", { name: /Next round/ }));

    expect(await screen.findByText(RIVER_PROMPT)).toBeInTheDocument();
    expect(roundGets(fetchMock, 1).length).toBeGreaterThanOrEqual(1);
    expect(screen.getByRole("button", { name: "Round 2: current" })).toBeInTheDocument();
  });

  it("prefetches the next round after the answer, so Next round needs no further fetch", async () => {
    const fetchMock = fakeServer(["TRIPOINT", "RIVERS_BY_SHAPE"]);
    const user = userEvent.setup();
    renderPage();
    await screen.findByText(TRIPOINT_PROMPT);
    expect(roundGets(fetchMock, 1)).toHaveLength(0);

    await solveTripoint(user);
    const next = await screen.findByRole("button", { name: /Next round/ });
    await waitFor(() => expect(roundGets(fetchMock, 1)).toHaveLength(1));
    await user.click(next);

    expect(await screen.findByText(RIVER_PROMPT)).toBeInTheDocument();
    expect(roundGets(fetchMock, 1)).toHaveLength(1);
  });

  it("prefetches the next round of a round that was already answered", async () => {
    const fetchMock = fakeServer(["TRIPOINT", "RIVERS_BY_SHAPE"], ["CORRECT", "GAVE_UP"]);
    renderPage();
    await screen.findByText(/Next round/);

    await waitFor(() => expect(roundGets(fetchMock, 1)).toHaveLength(1));
  });

  it("resumes at the first unanswered round after a reload", async () => {
    fakeServer(["TRIPOINT", "RIVERS_BY_SHAPE", "TRIPOINT"], ["CORRECT", "UNANSWERED", "UNANSWERED"]);
    renderPage();
    expect(await screen.findByText(RIVER_PROMPT)).toBeInTheDocument();
  });

  it("jumps to a round with the progress dots", async () => {
    const fetchMock = fakeServer(["TRIPOINT", "RIVERS_BY_SHAPE", "TRIPOINT"], ["CORRECT", "GAVE_UP", "UNANSWERED"]);
    renderPage();
    await screen.findByText(TRIPOINT_PROMPT);

    await userEvent.setup().click(screen.getByRole("button", { name: "Round 2: revealed" }));

    expect(await screen.findByText(RIVER_PROMPT)).toBeInTheDocument();
    expect(roundGets(fetchMock, 1).length).toBeGreaterThanOrEqual(1);
  });
});

describe("GameRoundPage results", () => {
  it("shows the summary after the last round and can return to a round from it", async () => {
    fakeServer(["TRIPOINT", "RIVERS_BY_SHAPE"], ["CORRECT", "GAVE_UP"]);
    const user = userEvent.setup();
    renderPage();
    await screen.findByText(TRIPOINT_PROMPT);

    await user.click(screen.getByRole("button", { name: "Round 2: revealed" }));
    await screen.findByText(RIVER_PROMPT);
    await user.click(screen.getByRole("button", { name: "See results" }));

    expect(await screen.findByText("You scored 100 / 200")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: /1\. Tripoint/ }));

    expect(await screen.findByText(TRIPOINT_PROMPT)).toBeInTheDocument();
    expect(screen.queryByText(/You scored/)).not.toBeInTheDocument();
  });

  it("falls back to the current round's state when the backend sends no round states", async () => {
    // The older backend can't say which rounds are answered, so the page opens the first round and reads its state.
    mockFetch([
      {
        url: /\/rounds\/0$/,
        body: {
          ...makeRound({ answerState: "CORRECT", totalRoundCount: 3 }),
          gameResult: { gameState: "IN_PROGRESS", roundsUnanswered: 2, score: 1 },
        },
      },
      {
        url: /\/rounds\/1$/,
        body: {
          ...makeRound({ answerState: "UNANSWERED", totalRoundCount: 3 }),
          gameResult: { gameState: "IN_PROGRESS", roundsUnanswered: 2, score: 1 },
        },
      },
      { url: /\/api\/countries$/, body: countryNames },
    ]);
    renderPage();
    await screen.findByText(TRIPOINT_PROMPT);

    expect(screen.getAllByRole("button", { name: /^Round \d/ })).toHaveLength(3);
    expect(screen.getByRole("button", { name: "Round 2: current" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Round 3: not played" })).toBeDisabled();
  });
});
