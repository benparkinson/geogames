import { act, fireEvent, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { callsTo, mockFetch } from "../test/fetchMock";
import { renderWithClient } from "../test/render";
import HomePage from "./HomePage";
import { localDateString } from "../api/daily";
import { resetDailyStorageForTests, writeDaily } from "../daily/dailyStorage";

const router = vi.hoisted(() => ({ push: vi.fn(), query: {} }));
vi.mock("next/router", () => ({ useRouter: () => router }));

const HEALTH = "http://localhost:8080/health";

beforeEach(() => {
  router.push.mockClear();
});

afterEach(() => {
  vi.useRealTimers();
});

function renderHome() {
  return renderWithClient(<HomePage />);
}

const cards = () => within(screen.getByRole("radiogroup", { name: "Choose a game" }));
const card = (name: RegExp) => cards().getByRole("radio", { name });
const startButton = () => screen.getByRole("button", { name: /Start game|Starting/ });

describe("HomePage on load", () => {
  it("wakes the server with a no-cors health request", () => {
    const fetchMock = mockFetch([{ url: HEALTH, body: "ok" }]);
    renderHome();

    expect(fetchMock).toHaveBeenCalledWith(HEALTH, { mode: "no-cors" });
  });

  it("still renders when the health request fails", () => {
    mockFetch([{ url: HEALTH, networkError: true }]);
    renderHome();

    expect(screen.getByRole("heading", { name: "GeoGauntlet", level: 1 })).toBeInTheDocument();
  });

  it("shows the site footer", () => {
    mockFetch([{ url: HEALTH, body: "ok" }]);
    renderHome();

    expect(screen.getByRole("contentinfo")).toBeInTheDocument();
  });
});

describe("HomePage choosing a game", () => {
  beforeEach(() => {
    mockFetch([{ url: HEALTH, body: "ok" }]);
  });

  it("selects no game on load, and shows no start button", () => {
    renderHome();

    for (const radio of cards().getAllByRole("radio")) {
      expect(radio).toHaveAttribute("aria-checked", "false");
    }
    expect(screen.queryByRole("button", { name: /Start game/ })).toBeNull();
    expect(card(/Tripoints/)).toHaveAttribute("tabindex", "0");
    expect(card(/River Shapes/)).toHaveAttribute("tabindex", "-1");
  });

  it("shows the start panel with the chosen game once one is selected", async () => {
    const user = userEvent.setup();
    renderHome();

    await user.click(card(/River Shapes/));
    const panel = screen.getByRole("complementary", { name: "Start a game" });
    expect(within(panel).getByText("River Shapes")).toBeInTheDocument();
    // The three round counts and the three difficulties.
    expect(within(panel).getAllByRole("radio")).toHaveLength(6);
    expect(within(panel).getByRole("button", { name: "Start game" })).toBeInTheDocument();

    await user.click(card(/Lake Shapes/));
    expect(
      within(screen.getByRole("complementary", { name: "Start a game" })).getByText("Lake Shapes"),
    ).toBeInTheDocument();
    expect(screen.getAllByRole("button", { name: "Start game" })).toHaveLength(1);
  });

  it("selecting with the arrow keys also shows the start panel", async () => {
    const user = userEvent.setup();
    renderHome();
    card(/Tripoints/).focus();

    await user.keyboard("{ArrowRight}");

    expect(
      within(screen.getByRole("complementary", { name: "Start a game" })).getByText("Country Cities"),
    ).toBeInTheDocument();
    expect(card(/Country Cities/)).toHaveFocus();
  });

  it("keeps the start panel before the Ko-fi link in the tab order", async () => {
    renderHome();
    await userEvent.setup().click(card(/Tripoints/));

    const kofi = screen.getByRole("link", { name: /Buy me a coffee/ });
    expect(startButton().compareDocumentPosition(kofi) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
  });

  it("styles the Ko-fi link quietly and opens it safely in a new tab", () => {
    renderHome();

    const kofi = screen.getByRole("link", { name: /Buy me a coffee/ });
    expect(kofi).toHaveClass("gg-btn--quiet");
    expect(kofi).not.toHaveClass("text-white");
    expect(kofi).toHaveAttribute("href", "https://ko-fi.com/O3J02ABGOW");
    expect(kofi).toHaveAttribute("target", "_blank");
    expect(kofi.getAttribute("rel")).toContain("noopener");
  });

  it("selects a card when it is clicked", async () => {
    renderHome();

    await userEvent.setup().click(card(/River Shapes/));

    expect(card(/River Shapes/)).toHaveAttribute("aria-checked", "true");
    expect(card(/Tripoints/)).toHaveAttribute("aria-checked", "false");
  });

  it("moves the selection and focus with the arrow keys across both sections, wrapping around", async () => {
    const user = userEvent.setup();
    renderHome();
    card(/Tripoints/).focus();

    const order = [
      /Country Cities/,
      /Merged Countries/,
      /Country Gaps/,
      /River Countries/,
      /Land Borders/,
      /Country Rivers/,
      /Coastlines/,
      /Mixed/,
      /River Shapes/,
      /Lake Shapes/,
      /Border Shapes/,
      /Tripoints/,
    ];
    for (const [i, name] of order.entries()) {
      await user.keyboard(i % 2 === 0 ? "{ArrowRight}" : "{ArrowDown}");
      expect(card(name)).toHaveAttribute("aria-checked", "true");
      expect(card(name)).toHaveFocus();
    }

    for (const name of [/Border Shapes/, /Lake Shapes/, /River Shapes/, /Mixed/]) {
      await user.keyboard("{ArrowLeft}");
      expect(card(name)).toHaveAttribute("aria-checked", "true");
      expect(card(name)).toHaveFocus();
    }

    await user.keyboard("{ArrowUp}");
    expect(card(/Coastlines/)).toHaveAttribute("aria-checked", "true");
  });

  it("puts the shape games in an extra difficult section", () => {
    renderHome();

    const extra = within(screen.getByRole("group", { name: "Extra difficult" }));
    expect(extra.getAllByRole("radio").map((radio) => radio.textContent)).toEqual([
      expect.stringContaining("River Shapes"),
      expect.stringContaining("Lake Shapes"),
      expect.stringContaining("Border Shapes"),
    ]);
  });

  it("can select an extra difficult game", async () => {
    renderHome();

    await userEvent.setup().click(card(/Lake Shapes/));

    expect(card(/Lake Shapes/)).toHaveAttribute("aria-checked", "true");
    expect(card(/Tripoints/)).toHaveAttribute("aria-checked", "false");
  });

  it("ignores other keys", async () => {
    const user = userEvent.setup();
    renderHome();
    card(/Tripoints/).focus();

    await user.keyboard("a");

    for (const radio of cards().getAllByRole("radio")) {
      expect(radio).toHaveAttribute("aria-checked", "false");
    }
    expect(screen.queryByRole("button", { name: /Start game/ })).toBeNull();
  });
});

describe("HomePage starting a game", () => {
  it("defaults to five rounds", async () => {
    mockFetch([{ url: HEALTH, body: "ok" }]);
    renderHome();
    await userEvent.setup().click(card(/Tripoints/));

    expect(screen.getByRole("radio", { name: "5" })).toBeChecked();
  });

  it("creates the chosen game with the chosen rounds and opens it", async () => {
    const fetchMock = mockFetch([
      { url: HEALTH, body: "ok" },
      { method: "POST", url: "http://localhost:8080/api/games", body: { id: "0b7c2a8e-4f1d-4c3a-9e5b-1d2f3a4b5c6d" } },
    ]);
    const user = userEvent.setup();
    renderHome();

    await user.click(card(/Mixed/));
    await user.click(screen.getByRole("radio", { name: "10" }));
    await user.click(startButton());

    await waitFor(() => expect(router.push).toHaveBeenCalledWith("/games/0b7c2a8e-4f1d-4c3a-9e5b-1d2f3a4b5c6d"));
    expect(callsTo(fetchMock).find((c) => c.method === "POST")!.body).toEqual({
      gameType: "RANDOM",
      numberOfRounds: 10,
      difficulty: "NORMAL",
    });
  });

  it("starts the chosen game with five rounds by default", async () => {
    const fetchMock = mockFetch([
      { url: HEALTH, body: "ok" },
      { method: "POST", url: /\/api\/games$/, body: { id: "0b7c2a8e-4f1d-4c3a-9e5b-1d2f3a4b5c6d" } },
    ]);
    const user = userEvent.setup();
    renderHome();

    await user.click(card(/Tripoints/));
    await user.click(startButton());

    await waitFor(() => expect(router.push).toHaveBeenCalled());
    expect(callsTo(fetchMock).find((c) => c.method === "POST")!.body).toEqual({
      gameType: "TRIPOINT",
      numberOfRounds: 5,
      difficulty: "NORMAL",
    });
  });

  it("disables the button and says it is starting while waiting", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn((url: string) => (url === HEALTH ? Promise.resolve({}) : new Promise(() => {}))),
    );
    renderHome();

    const user = userEvent.setup();
    await user.click(card(/Tripoints/));
    await user.click(startButton());

    expect(await screen.findByRole("button", { name: /Starting/ })).toBeDisabled();
  });

  it("explains a slow server after three seconds, and stops once the request ends", async () => {
    vi.useFakeTimers();
    let finish: (value: unknown) => void = () => {};
    vi.stubGlobal(
      "fetch",
      vi.fn((url: string) => (url === HEALTH ? Promise.resolve({}) : new Promise((resolve) => (finish = resolve)))),
    );
    renderHome();
    // Testing Library only detects Jest's fake timers, so user-event would hang here: use fireEvent.
    fireEvent.click(card(/Tripoints/));
    fireEvent.click(startButton());
    const message = /Waking up the server/;

    // React Query delivers the pending state through a zero-delay timer, so flush that first.
    await act(async () => {
      vi.advanceTimersByTime(0);
    });
    expect(screen.getByRole("button", { name: /Starting/ })).toBeDisabled();

    await act(async () => {
      vi.advanceTimersByTime(2900);
    });
    expect(screen.queryByText(message)).not.toBeInTheDocument();

    await act(async () => {
      vi.advanceTimersByTime(200);
    });
    expect(screen.getByText(message)).toBeInTheDocument();

    await act(async () => {
      finish({ ok: true, status: 200, json: async () => ({ id: "0b7c2a8e-4f1d-4c3a-9e5b-1d2f3a4b5c6d" }) });
    });
    await vi.waitFor(() => expect(screen.queryByText(message)).not.toBeInTheDocument());
  });

  it("shows an alert when the game could not be created", async () => {
    mockFetch([
      { url: HEALTH, body: "ok" },
      { method: "POST", url: /\/api\/games$/, status: 500, body: {} },
    ]);
    renderHome();

    const user = userEvent.setup();
    await user.click(card(/Tripoints/));
    await user.click(startButton());

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Couldn't start a game. The server may be down. Try again.",
    );
    expect(router.push).not.toHaveBeenCalled();
  });
});

describe("HomePage difficulty", () => {
  it("has a control that chooses Easy, Normal or Hard and remembers it", async () => {
    mockFetch([{ url: HEALTH, body: "ok" }]);
    const user = userEvent.setup();
    renderHome();
    await user.click(card(/Country Gaps/));

    const group = screen.getByRole("group", { name: "Difficulty" });
    expect(within(group).getByRole("radio", { name: "Normal" })).toBeChecked();

    await user.click(within(group).getByRole("radio", { name: "Easy" }));
    expect(within(group).getByRole("radio", { name: "Easy" })).toBeChecked();
    expect(localStorage.getItem("gg-difficulty")).toBe("easy");
    expect(screen.getByText("The continent is shown and you get more guesses.")).toBeInTheDocument();

    await user.click(within(group).getByRole("radio", { name: "Hard" }));
    expect(localStorage.getItem("gg-difficulty")).toBe("hard");

    await user.click(within(group).getByRole("radio", { name: "Normal" }));
    expect(localStorage.getItem("gg-difficulty")).toBeNull();
  });

  it("shows the control for every game", async () => {
    mockFetch([{ url: HEALTH, body: "ok" }]);
    const user = userEvent.setup();
    renderHome();
    expect(screen.queryByRole("group", { name: "Difficulty" })).toBeNull();

    for (const game of [/Tripoints/, /Mixed/, /River Shapes/, /Merged Countries/]) {
      await user.click(card(game));
      expect(screen.getByRole("group", { name: "Difficulty" })).toBeInTheDocument();
    }
  });
});

describe("HomePage difficulty when starting", () => {
  async function startWith(stored: string | null, game: RegExp) {
    if (stored !== null) localStorage.setItem("gg-difficulty", stored);
    const fetchMock = mockFetch([
      { url: HEALTH, body: "ok" },
      { method: "POST", url: /\/api\/games$/, body: { id: "0b7c2a8e-4f1d-4c3a-9e5b-1d2f3a4b5c6d" } },
    ]);
    const user = userEvent.setup();
    renderHome();
    await user.click(card(game));
    await user.click(startButton());
    await waitFor(() => expect(router.push).toHaveBeenCalled());
    return callsTo(fetchMock).find((c) => c.method === "POST")!.body as { difficulty: string };
  }

  it("sends Hard for a game with clues", async () => {
    expect((await startWith("hard", /Country Gaps/)).difficulty).toBe("HARD");
  });

  it("sends Hard for a Mixed game", async () => {
    expect((await startWith("hard", /Mixed/)).difficulty).toBe("HARD");
  });

  it("sends Normal for a game without clues when Hard is chosen", async () => {
    expect((await startWith("hard", /Tripoints/)).difficulty).toBe("NORMAL");
  });

  it("sends Easy for a game without clues", async () => {
    expect((await startWith("easy", /Tripoints/)).difficulty).toBe("EASY");
  });

  it("sends Easy for a game with clues", async () => {
    expect((await startWith("easy", /Country Gaps/)).difficulty).toBe("EASY");
  });

  it("treats an old Hard mode preference as Hard", async () => {
    localStorage.setItem("gg-hardcore", "on");

    expect((await startWith(null, /Country Gaps/)).difficulty).toBe("HARD");
  });
});

describe("HomePage daily challenge card", () => {
  const entry = { v: 1 as const, date: localDateString(), number: 2, gameId: "g1", difficulty: "NORMAL" as const };

  beforeEach(resetDailyStorageForTests);

  it("links to /daily, outside the game cards, without creating a game", () => {
    const fetchMock = mockFetch([{ url: HEALTH, body: "ok" }]);
    renderHome();

    const link = screen.getByRole("link", { name: /Daily challenge/ });
    expect(link).toHaveAttribute("href", "/daily");
    expect(cards().queryByRole("link", { name: /Daily challenge/ })).toBeNull();
    expect(callsTo(fetchMock).filter((c) => c.method === "POST")).toEqual([]);
  });

  it("says Continue for a game started today", () => {
    mockFetch([{ url: HEALTH, body: "ok" }]);
    writeDaily(entry);
    renderHome();

    expect(screen.getByRole("link", { name: /Daily challenge/ })).toHaveTextContent("Continue");
  });

  it("shows the score once today's game is finished", () => {
    mockFetch([{ url: HEALTH, body: "ok" }]);
    writeDaily({
      ...entry,
      result: {
        score: 430,
        maxScore: 500,
        rounds: ["firstGuess", "firstGuess", "firstGuess", "firstGuess", "firstGuess"],
      },
    });
    renderHome();

    expect(screen.getByRole("link", { name: /Daily challenge/ })).toHaveTextContent("Done: 430 / 500");
  });

  it("ignores a game from another day", () => {
    mockFetch([{ url: HEALTH, body: "ok" }]);
    writeDaily({ ...entry, date: "2020-01-01" });
    renderHome();

    expect(screen.getByRole("link", { name: /Daily challenge/ })).not.toHaveTextContent("Continue");
  });
});
