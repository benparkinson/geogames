import { screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { callsTo, mockFetch, Route } from "../../test/fetchMock";
import { makeRiverRound, makeRound } from "../../test/fixtures";
import { renderWithClient } from "../../test/render";
import { GameRoundResponse } from "../../api/types";
import GameSummary from "./GameSummary";

const router = vi.hoisted(() => ({ push: vi.fn(), query: {} }));
vi.mock("next/router", () => ({ useRouter: () => router }));

const roundsUrl = /\/api\/games\/g1\/rounds\/(\d+)$/;

function roundsRoute(rounds: GameRoundResponse[]): Route {
  return { url: roundsUrl, body: ({ url }) => rounds[Number(url.match(roundsUrl)![1])] };
}

const mixed = [
  makeRound({ answerState: "CORRECT" }),
  makeRiverRound({ answerState: "GAVE_UP" }),
  makeRound({ answerState: "CORRECT" }),
];

function renderSummary(onOpenRound = vi.fn(), totalRoundCount = 3) {
  const view = renderWithClient(
    <GameSummary gameId="g1" totalRoundCount={totalRoundCount} onOpenRound={onOpenRound} />,
  );
  return { ...view, onOpenRound };
}

describe("GameSummary", () => {
  it("shows a spinner while the rounds load", () => {
    vi.stubGlobal("fetch", vi.fn(() => new Promise(() => {})));
    const { container } = renderSummary();

    expect(container.querySelector(".spinner-border")).not.toBeNull();
    expect(screen.queryByText(/You scored/)).not.toBeInTheDocument();
  });

  it("shows the score and one row per round", async () => {
    mockFetch([roundsRoute(mixed)]);
    renderSummary();

    expect(await screen.findByText("You scored 2 / 3")).toBeInTheDocument();
    expect(screen.getByText("2 correct · 1 revealed")).toBeInTheDocument();

    const rows = screen.getAllByRole("button").filter((b) => /^\d\./.test(b.textContent ?? ""));
    expect(rows).toHaveLength(3);
    expect(within(rows[0]).getByText("1. Tripoint")).toBeInTheDocument();
    expect(rows[0]).toHaveTextContent("Belgium, Netherlands, Germany");
    expect(rows[0]).toHaveTextContent("Correct");
    expect(within(rows[1]).getByText("2. River")).toBeInTheDocument();
    expect(rows[1]).toHaveTextContent("Danube");
    expect(rows[1]).toHaveTextContent("Revealed");
  });

  it("opens the round that was clicked", async () => {
    mockFetch([roundsRoute(mixed)]);
    const { onOpenRound } = renderSummary();
    await screen.findByText("You scored 2 / 3");

    await userEvent.setup().click(screen.getByRole("button", { name: /2\. River/ }));

    expect(onOpenRound).toHaveBeenCalledWith(1);
  });

  it("links home from the header and from the button", async () => {
    mockFetch([roundsRoute(mixed)]);
    renderSummary();
    await screen.findByText("You scored 2 / 3");

    const links = screen.getAllByRole("link", { name: "Home" });
    expect(links).toHaveLength(2);
    links.forEach((link) => expect(link).toHaveAttribute("href", "/"));
  });

  it("plays again with a random mix when the game had different types", async () => {
    const fetchMock = mockFetch([
      roundsRoute(mixed),
      { method: "POST", url: /\/api\/games$/, body: { id: 12 } },
    ]);
    renderSummary();
    await screen.findByText("You scored 2 / 3");

    await userEvent.setup().click(screen.getByRole("button", { name: "Play again" }));

    await waitFor(() => expect(router.push).toHaveBeenCalledWith("/games/12"));
    const post = callsTo(fetchMock).find((c) => c.method === "POST")!;
    expect(post.body).toEqual({ gameType: "RANDOM", numberOfRounds: 3 });
  });

  it("plays again with the same type when every round shared it", async () => {
    const rivers = [makeRiverRound({ answerState: "CORRECT" }), makeRiverRound({ answerState: "CORRECT" })];
    const fetchMock = mockFetch([
      roundsRoute(rivers),
      { method: "POST", url: /\/api\/games$/, body: { id: 5 } },
    ]);
    renderSummary(vi.fn(), 2);
    await screen.findByText("You scored 2 / 2");

    await userEvent.setup().click(screen.getByRole("button", { name: "Play again" }));

    await waitFor(() => expect(callsTo(fetchMock).some((c) => c.method === "POST")).toBe(true));
    expect(callsTo(fetchMock).find((c) => c.method === "POST")!.body).toEqual({
      gameType: "RIVERS_BY_SHAPE",
      numberOfRounds: 2,
    });
  });

  it("tells the player when a new game could not be started", async () => {
    mockFetch([roundsRoute(mixed), { method: "POST", url: /\/api\/games$/, status: 500, body: {} }]);
    renderSummary();
    await screen.findByText("You scored 2 / 3");

    await userEvent.setup().click(screen.getByRole("button", { name: "Play again" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Couldn't start a new game. Try again.");
  });
});
