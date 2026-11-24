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

const TRIPOINT_PROMPT = "Which 3 countries meet at this point?";
const RIVER_PROMPT = "Which river is this?";

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
  const saved: { text: string; correct: boolean }[][] = kinds.map(() => []);
  const taken: number[][] = kinds.map(() => []);
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
    {
      method: "POST",
      url: /\/api\/games\/g1\/rounds\/\d+\/hints$/,
      body: ({ url, init }) => {
        const index = Number(url.match(/rounds\/(\d+)\/hints/)![1]);
        taken[index].push(JSON.parse(init!.body as string).guessesBefore);
        return { hintsTakenAt: taken[index] };
      },
    },
    {
      method: "POST",
      url: /\/api\/games\/g1\/rounds\/\d+\/guesses$/,
      body: ({ url, init }) => {
        const index = Number(url.match(/rounds\/(\d+)\/guesses/)![1]);
        const body = JSON.parse(init!.body as string);
        saved[index].push({ text: body.guess, correct: body.correct });
        return { guesses: saved[index] };
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
    vi.stubGlobal(
      "fetch",
      vi.fn(() => new Promise(() => {})),
    );
    renderPage();

    expect(screen.getByRole("status")).toBeTruthy();
    expect(title()).toBe("Loading... | GeoGauntlet");
  });

  it("shows an error when the round cannot be loaded", async () => {
    mockFetch([{ url: /\/rounds\/0$/, status: 500, body: {} }]);
    renderPage();

    expect(await screen.findByRole("alert")).toHaveTextContent("Couldn't load this round. Please refresh.");
  });
});

describe("GameRoundPage game types", () => {
  it("titles and tells a daily game by its date", async () => {
    fakeServer(["TRIPOINT"]);
    render(
      <QueryClientProvider client={client}>
        <GameRoundPage gameId="g1" daily={{ date: "2026-11-18" }} />
      </QueryClientProvider>,
    );

    expect(await screen.findByText("Daily")).toBeInTheDocument();
    expect(title()).toBe("Daily 2026-11-18 | GeoGauntlet");
  });

  it("renders the tripoint game and title", async () => {
    fakeServer(["TRIPOINT"]);
    renderPage();

    expect(await screen.findByText(TRIPOINT_PROMPT)).toBeInTheDocument();
    expect(title()).toBe("Tripoints | GeoGauntlet");
  });

  it("renders the river game and title", async () => {
    fakeServer(["RIVERS_BY_SHAPE"]);
    renderPage();

    expect(await screen.findByText(RIVER_PROMPT)).toBeInTheDocument();
    expect(title()).toBe("River Shapes | GeoGauntlet");
  });

  it("copes with a game type it does not know", async () => {
    mockFetch([
      {
        url: /\/rounds\/0$/,
        body: makeRound({ gameType: { name: "MYSTERY", answerCount: 1, hintCount: 3, guessLimit: 5 } }),
      },
    ]);
    renderPage();

    expect(await screen.findByText("Unknown game type...")).toBeInTheDocument();
    expect(title()).toBe("Unknown game! | GeoGauntlet");
  });
});

describe("GameRoundPage answer saving", () => {
  it("ignores a jump to another round while the answer is still being saved", async () => {
    const server = fakeServer(["TRIPOINT", "TRIPOINT"]);
    const working = server.getMockImplementation()!;
    let release: () => void = () => {};
    const held = new Promise<void>((resolve) => (release = resolve));
    vi.stubGlobal(
      "fetch",
      vi.fn(async (url: string, init?: RequestInit) => {
        if (String(url).endsWith("/rounds/0/answers")) {
          await held;
        }
        return working(url, init);
      }),
    );
    const user = userEvent.setup();
    renderPage();
    await screen.findByText(TRIPOINT_PROMPT);

    await solveTripoint(user);

    expect(await screen.findByRole("button", { name: /Next round/ })).toBeDisabled();
    await user.click(screen.getByRole("button", { name: "Round 1: current" }));
    expect(screen.getByRole("button", { name: /Next round/ })).toBeDisabled();

    release();
    await waitFor(() => expect(screen.getByRole("button", { name: /Next round/ })).toBeEnabled());
  });

  it("holds the player on the round until the answer is saved, and retries a failed save", async () => {
    let attempts = 0;
    const server = fakeServer(["TRIPOINT", "TRIPOINT"]);
    const working = server.getMockImplementation()!;
    vi.stubGlobal(
      "fetch",
      vi.fn((url: string, init?: RequestInit) => {
        if (String(url).endsWith("/rounds/0/answers") && attempts++ === 0) {
          return Promise.resolve(new Response("{}", { status: 500 }));
        }
        return working(url, init);
      }),
    );
    const user = userEvent.setup();
    renderPage();
    await screen.findByText(TRIPOINT_PROMPT);

    await solveTripoint(user);

    await user.click(await screen.findByRole("button", { name: "Try again" }));
    const next = await screen.findByRole("button", { name: /Next round/ });
    await waitFor(() => expect(next).toBeEnabled());
    await user.click(next);

    expect(await screen.findByRole("button", { name: "Round 2: current" })).toBeInTheDocument();
    expect(attempts).toBe(2);
  });
});

describe("GameRoundPage playing", () => {
  it("posts the answer and patches the round instead of reloading it", async () => {
    const fetchMock = fakeServer(["TRIPOINT", "TRIPOINT"]);
    const user = userEvent.setup();
    renderPage();
    await screen.findByText(TRIPOINT_PROMPT);
    expect(roundGets(fetchMock, 0)).toHaveLength(1);

    await solveTripoint(user);

    await waitFor(() => {
      const posts = callsTo(fetchMock).filter((c) => c.method === "POST" && c.url.endsWith("/answers"));
      expect(posts).toHaveLength(1);
      expect(posts[0].url).toBe("http://localhost:8080/api/games/g1/rounds/0/answers");
      expect(posts[0].body).toEqual({ answerState: "CORRECT", hintsUsed: 0, wrongGuesses: 0, answersFound: 3 });
    });
    // The next round is prefetched once the answer is patched in; the answered round is not fetched again.
    await waitFor(() => expect(roundGets(fetchMock, 1)).toHaveLength(1));
    expect(roundGets(fetchMock, 0)).toHaveLength(1);
  });

  it("saves a hint as it is taken", async () => {
    const fetchMock = fakeServer(["TRIPOINT"]);
    const user = userEvent.setup();
    renderPage();
    await screen.findByText(TRIPOINT_PROMPT);

    await user.click(screen.getByRole("button", { name: /Hint .*· 3 left/ }));

    await waitFor(() => {
      const posts = callsTo(fetchMock).filter((c) => c.method === "POST");
      expect(posts).toHaveLength(1);
      expect(posts[0].url).toBe("http://localhost:8080/api/games/g1/rounds/0/hints");
      expect(posts[0].body).toEqual({ hint: 0, guessesBefore: 0 });
    });
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

  it("names the round after its type when a mixed game moves to a round of another type", async () => {
    fakeServer(["TRIPOINT", "RIVERS_BY_SHAPE"]);
    const user = userEvent.setup();
    renderPage();
    await screen.findByText(TRIPOINT_PROMPT);
    expect(screen.getAllByText("Tripoints").length).toBeGreaterThan(0);

    await solveTripoint(user);
    await user.click(await screen.findByRole("button", { name: /Next round/ }));

    await screen.findByText(RIVER_PROMPT);
    expect(screen.getAllByText("River Shapes").length).toBeGreaterThan(0);
    expect(screen.queryAllByText("Tripoints")).toHaveLength(0);
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
    const fetchMock = fakeServer(["TRIPOINT", "RIVERS_BY_SHAPE", "TRIPOINT"], ["CORRECT", "UNANSWERED", "UNANSWERED"]);
    renderPage();
    await screen.findByText(RIVER_PROMPT);

    await waitFor(() => expect(roundGets(fetchMock, 1)).toHaveLength(1));
  });

  it("resumes at the first unanswered round after a reload", async () => {
    fakeServer(["TRIPOINT", "RIVERS_BY_SHAPE", "TRIPOINT"], ["CORRECT", "UNANSWERED", "UNANSWERED"]);
    renderPage();
    expect(await screen.findByText(RIVER_PROMPT)).toBeInTheDocument();
  });

  it("opens a finished game on its results after a reload", async () => {
    fakeServer(["TRIPOINT", "RIVERS_BY_SHAPE"], ["CORRECT", "GAVE_UP"]);
    renderPage();

    expect(await screen.findByRole("heading", { name: "Your result" })).toBeInTheDocument();
    expect(screen.queryByText(TRIPOINT_PROMPT)).not.toBeInTheDocument();
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
    expect(await screen.findByRole("heading", { name: "Your result" })).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: /Belgium/ }));

    expect(await screen.findByText(TRIPOINT_PROMPT)).toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: "Your result" })).not.toBeInTheDocument();
  });

  it("returns to the results from a round with See results", async () => {
    fakeServer(["TRIPOINT", "RIVERS_BY_SHAPE"], ["CORRECT", "GAVE_UP"]);
    const user = userEvent.setup();
    renderPage();
    await screen.findByRole("heading", { name: "Your result" });

    await user.click(screen.getByRole("button", { name: /Belgium/ }));
    await screen.findByText(TRIPOINT_PROMPT);
    await user.click(screen.getByRole("button", { name: "Round 2: revealed" }));
    await screen.findByText(RIVER_PROMPT);
    await user.click(screen.getByRole("button", { name: "See results" }));

    expect(await screen.findByRole("heading", { name: "Your result" })).toBeInTheDocument();
  });
});
