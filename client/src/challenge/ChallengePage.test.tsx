import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { callsTo, mockFetch } from "../test/fetchMock";
import { makeChallengeResponse } from "../test/fixtures";
import { renderWithClient } from "../test/render";
import ChallengePage from "./ChallengePage";
import { resetChallengeStorageForTests } from "./challengeStorage";

const router = vi.hoisted(() => ({ replace: vi.fn(), push: vi.fn(), query: {} }));
vi.mock("next/router", () => ({ useRouter: () => router }));

const API = "http://localhost:8080";
const INFO = API + "/api/challenges/c1";
const PLAY = API + "/api/challenges/c1/games";
const HEALTH = API + "/health";

beforeEach(() => {
  localStorage.clear();
  resetChallengeStorageForTests();
  router.replace.mockClear();
});

const wake = { url: HEALTH, body: {} };

describe("ChallengePage", () => {
  it("shows the settings and the score to beat, and starts nothing on load", async () => {
    const fetchMock = mockFetch([{ url: INFO, body: makeChallengeResponse({ difficulty: "HARD" }) }, wake]);

    renderWithClient(<ChallengePage id="c1" />);

    expect(await screen.findByText("They scored 320 / 500. Can you beat it?")).toBeInTheDocument();
    expect(screen.getByText("Mixed · 5 rounds · Hard mode")).toBeInTheDocument();
    expect(callsTo(fetchMock).filter((c) => c.method === "POST")).toHaveLength(0);
  });

  it("says the challenge has expired, with a way home, on a 404", async () => {
    mockFetch([{ url: INFO, status: 404, body: {} }, wake]);

    renderWithClient(<ChallengePage id="c1" />);

    expect(await screen.findByRole("heading", { name: "This challenge has expired" })).toBeInTheDocument();
    expect(screen.getAllByRole("link", { name: "Home" }).every((l) => l.getAttribute("href") === "/")).toBe(true);
  });

  it("Play makes a game, remembers the score to beat and replaces the route", async () => {
    mockFetch([
      { url: INFO, body: makeChallengeResponse() },
      { method: "POST", url: PLAY, body: { id: "new-game" } },
      wake,
    ]);

    renderWithClient(<ChallengePage id="c1" />);
    await userEvent.click(await screen.findByRole("button", { name: "Play" }));

    await waitFor(() => expect(router.replace).toHaveBeenCalledWith("/games/new-game"));
    expect(JSON.parse(localStorage.getItem("gg-challenges")!)).toEqual([
      { gameId: "new-game", score: 320, maxScore: 500 },
    ]);
  });

  it("shows an error and stays when the game can't be made", async () => {
    mockFetch([
      { url: INFO, body: makeChallengeResponse() },
      { method: "POST", url: PLAY, status: 429, body: {} },
      wake,
    ]);

    renderWithClient(<ChallengePage id="c1" />);
    await userEvent.click(await screen.findByRole("button", { name: "Play" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Too many new games");
    expect(router.replace).not.toHaveBeenCalled();
  });

  it("says it couldn't load the challenge when the server is down", async () => {
    mockFetch([{ url: INFO, status: 500, body: {} }, wake]);

    renderWithClient(<ChallengePage id="c1" />);

    expect(await screen.findByRole("heading", { name: "Couldn't load the challenge" })).toBeInTheDocument();
  });

  it("shows Starting while the game is made, and names a mode it doesn't know as it is", async () => {
    mockFetch([
      { url: INFO, body: makeChallengeResponse({ gameMode: "NEW_MODE", roundCount: 1 }) },
      { method: "POST", url: PLAY, body: () => new Promise(() => {}) },
      wake,
    ]);

    renderWithClient(<ChallengePage id="c1" />);
    expect(await screen.findByText(/^NEW_MODE .+ 1 round$/)).toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: "Play" }));

    expect(await screen.findByRole("button", { name: /Starting/ })).toBeDisabled();
  });
});
