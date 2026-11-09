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

    expect(screen.getByRole("heading", { name: "GeoGauntlet", level: 1 })).toBeInTheDocument();
  });

  it("credits the data, imagery and icons, and links to the software licences", () => {
    mockFetch([{ url: HEALTH, body: "ok" }]);
    renderHome();

    const footer = within(screen.getByRole("contentinfo"));
    const href = (name: string) => footer.getByRole("link", { name }).getAttribute("href");
    expect(href("Natural Earth")).toBe("https://www.naturalearthdata.com/");
    expect(href("Wikidata")).toBe("https://www.wikidata.org/");
    expect(href("Wikipedia")).toBe("https://en.wikipedia.org/wiki/List_of_tripoints");
    expect(href("CC BY-SA 4.0")).toBe("https://creativecommons.org/licenses/by-sa/4.0/");
    expect(href("Esri")).toBe("https://www.esri.com/");
    expect(href("Earthz Stocker")).toContain("flaticon.com");
    expect(href("Freepik")).toContain("flaticon.com");
    expect(href("Software licences")).toBe("/third-party-licenses.txt");
    expect(footer.queryByText(/OpenStreetMap/)).not.toBeInTheDocument();
    expect(footer.getByText(/your light or dark mode choice/)).toBeInTheDocument();
  });

  it("tells players that anonymous round results are recorded", () => {
    mockFetch([{ url: HEALTH, body: "ok" }]);
    renderHome();

    const footer = within(screen.getByRole("contentinfo"));
    expect(footer.getByText(/We record anonymous round results \(no cookies, no personal data\)/)).toBeInTheDocument();
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
    expect(within(panel).getAllByRole("radio")).toHaveLength(3);
    expect(within(panel).getByRole("button", { name: "Start game" })).toBeInTheDocument();

    await user.click(card(/Lake Shapes/));
    expect(within(screen.getByRole("complementary", { name: "Start a game" })).getByText("Lake Shapes")).toBeInTheDocument();
    expect(screen.getAllByRole("button", { name: "Start game" })).toHaveLength(1);
  });

  it("selecting with the arrow keys also shows the start panel", async () => {
    const user = userEvent.setup();
    renderHome();
    card(/Tripoints/).focus();

    await user.keyboard("{ArrowRight}");

    expect(within(screen.getByRole("complementary", { name: "Start a game" })).getByText("Country Gaps")).toBeInTheDocument();
    expect(card(/Country Gaps/)).toHaveFocus();
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
    expect(kofi).toHaveClass("btn-outline-secondary");
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

    const order = [/Country Gaps/, /Land Borders/, /River Countries/, /Coastlines/, /Mixed/, /River Shapes/, /Lake Shapes/, /Tripoints/];
    for (const [i, name] of order.entries()) {
      await user.keyboard(i % 2 === 0 ? "{ArrowRight}" : "{ArrowDown}");
      expect(card(name)).toHaveAttribute("aria-checked", "true");
      expect(card(name)).toHaveFocus();
    }

    for (const name of [/Lake Shapes/, /River Shapes/, /Mixed/, /Coastlines/]) {
      await user.keyboard("{ArrowLeft}");
      expect(card(name)).toHaveAttribute("aria-checked", "true");
      expect(card(name)).toHaveFocus();
    }

    await user.keyboard("{ArrowUp}");
    expect(card(/River Countries/)).toHaveAttribute("aria-checked", "true");
  });

  it("puts the shape games in an extra difficult section", () => {
    renderHome();

    const extra = within(screen.getByRole("group", { name: "Extra difficult" }));
    expect(extra.getAllByRole("radio").map((radio) => radio.textContent)).toEqual([
      expect.stringContaining("River Shapes"),
      expect.stringContaining("Lake Shapes"),
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
    });
  });

  it("disables the button and says it is starting while waiting", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn((url: string) => (url === HEALTH ? Promise.resolve({}) : new Promise(() => { }))),
    );
    renderHome();

    const user = userEvent.setup();
    await user.click(card(/Tripoints/));
    await user.click(startButton());

    expect(await screen.findByRole("button", { name: /Starting/ })).toBeDisabled();
  });

  it("explains a slow server after three seconds, and stops once the request ends", async () => {
    vi.useFakeTimers();
    let finish: (value: unknown) => void = () => { };
    vi.stubGlobal(
      "fetch",
      vi.fn((url: string) =>
        url === HEALTH ? Promise.resolve({}) : new Promise((resolve) => (finish = resolve)),
      ),
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
