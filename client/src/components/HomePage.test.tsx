import { act, fireEvent, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { callsTo, mockFetch } from "../test/fetchMock";
import { renderWithClient } from "../test/render";
import HomePage from "./HomePage";

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

    expect(screen.getByRole("heading", { name: "Geogames", level: 1 })).toBeInTheDocument();
  });
});

describe("HomePage choosing a game", () => {
  beforeEach(() => {
    mockFetch([{ url: HEALTH, body: "ok" }]);
  });

  it("selects tripoints by default, and only one card at a time", () => {
    renderHome();

    expect(card(/Tripoints/)).toHaveAttribute("aria-checked", "true");
    expect(card(/River Shapes/)).toHaveAttribute("aria-checked", "false");
    expect(card(/Mixed/)).toHaveAttribute("aria-checked", "false");
  });

  it("selects a card when it is clicked", async () => {
    renderHome();

    await userEvent.setup().click(card(/River Shapes/));

    expect(card(/River Shapes/)).toHaveAttribute("aria-checked", "true");
    expect(card(/Tripoints/)).toHaveAttribute("aria-checked", "false");
  });

  it("moves the selection and focus with the arrow keys, wrapping around", async () => {
    const user = userEvent.setup();
    renderHome();
    card(/Tripoints/).focus();

    await user.keyboard("{ArrowRight}");
    expect(card(/River Shapes/)).toHaveAttribute("aria-checked", "true");
    expect(card(/River Shapes/)).toHaveFocus();

    await user.keyboard("{ArrowDown}");
    expect(card(/Rivers on the Map/)).toHaveAttribute("aria-checked", "true");

    await user.keyboard("{ArrowRight}");
    expect(card(/River Ends/)).toHaveAttribute("aria-checked", "true");

    await user.keyboard("{ArrowRight}");
    expect(card(/Mixed/)).toHaveAttribute("aria-checked", "true");

    await user.keyboard("{ArrowRight}");
    expect(card(/Tripoints/)).toHaveAttribute("aria-checked", "true");
    expect(card(/Tripoints/)).toHaveFocus();

    await user.keyboard("{ArrowLeft}");
    expect(card(/Mixed/)).toHaveAttribute("aria-checked", "true");

    await user.keyboard("{ArrowUp}");
    expect(card(/River Ends/)).toHaveAttribute("aria-checked", "true");

    await user.keyboard("{ArrowUp}");
    expect(card(/Rivers on the Map/)).toHaveAttribute("aria-checked", "true");
  });

  it("ignores other keys", async () => {
    const user = userEvent.setup();
    renderHome();
    card(/Tripoints/).focus();

    await user.keyboard("a");

    expect(card(/Tripoints/)).toHaveAttribute("aria-checked", "true");
  });
});

describe("HomePage starting a game", () => {
  it("defaults to five rounds", () => {
    mockFetch([{ url: HEALTH, body: "ok" }]);
    renderHome();

    expect(screen.getByRole("radio", { name: "5" })).toBeChecked();
  });

  it("creates the chosen game with the chosen rounds and opens it", async () => {
    const fetchMock = mockFetch([
      { url: HEALTH, body: "ok" },
      { method: "POST", url: "http://localhost:8080/api/games", body: { id: 9 } },
    ]);
    const user = userEvent.setup();
    renderHome();

    await user.click(card(/Mixed/));
    await user.click(screen.getByRole("radio", { name: "10" }));
    await user.click(startButton());

    await waitFor(() => expect(router.push).toHaveBeenCalledWith("/games/9"));
    expect(callsTo(fetchMock).find((c) => c.method === "POST")!.body).toEqual({
      gameType: "RANDOM",
      numberOfRounds: 10,
    });
  });

  it("starts tripoints with five rounds by default", async () => {
    const fetchMock = mockFetch([
      { url: HEALTH, body: "ok" },
      { method: "POST", url: /\/api\/games$/, body: { id: 1 } },
    ]);
    renderHome();

    await userEvent.setup().click(startButton());

    await waitFor(() => expect(router.push).toHaveBeenCalled());
    expect(callsTo(fetchMock).find((c) => c.method === "POST")!.body).toEqual({
      gameType: "TRIPOINT",
      numberOfRounds: 5,
    });
  });

  it("disables the button and says it is starting while waiting", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn((url: string) => (url === HEALTH ? Promise.resolve({}) : new Promise(() => {}))),
    );
    renderHome();

    await userEvent.setup().click(startButton());

    expect(await screen.findByRole("button", { name: /Starting/ })).toBeDisabled();
  });

  it("explains a slow server after three seconds, and stops once the request ends", async () => {
    vi.useFakeTimers();
    let finish: (value: unknown) => void = () => {};
    vi.stubGlobal(
      "fetch",
      vi.fn((url: string) =>
        url === HEALTH ? Promise.resolve({}) : new Promise((resolve) => (finish = resolve)),
      ),
    );
    renderHome();
    // Testing Library only detects Jest's fake timers, so user-event would hang here: use fireEvent.
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
      finish({ ok: true, status: 200, json: async () => ({ id: 4 }) });
    });
    await vi.waitFor(() => expect(screen.queryByText(message)).not.toBeInTheDocument());
  });

  it("shows an alert when the game could not be created", async () => {
    mockFetch([
      { url: HEALTH, body: "ok" },
      { method: "POST", url: /\/api\/games$/, status: 500, body: {} },
    ]);
    renderHome();

    await userEvent.setup().click(startButton());

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Couldn't start a game. The server may be down. Try again.",
    );
    expect(router.push).not.toHaveBeenCalled();
  });
});
