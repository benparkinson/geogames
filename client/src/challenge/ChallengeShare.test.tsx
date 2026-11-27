import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { callsTo, mockFetch } from "../test/fetchMock";
import { makeChallengeCreationResponse, makeChallengeTableResponse, makeGameRounds } from "../test/fixtures";
import { renderWithClient } from "../test/render";
import { toRoundView } from "../api/games";
import ChallengeShare from "./ChallengeShare";

const CREATE = "http://localhost:8080/api/games/g1/challenges";
const TABLE = "http://localhost:8080/api/games/g1/challenge-table";

afterEach(() => {
  Object.defineProperty(navigator, "clipboard", { value: undefined, configurable: true });
});

function finishedRound() {
  const rounds = makeGameRounds([{ answerState: "CORRECT" }, { answerState: "GAVE_UP" }]);
  return toRoundView(rounds[0]);
}

describe("ChallengeShare", () => {
  it("makes one challenge, then copies the text with the link on the first click", async () => {
    const writeText = vi.fn().mockResolvedValue(undefined);
    Object.defineProperty(navigator, "clipboard", { value: { writeText }, configurable: true });
    const fetchMock = mockFetch([
      { method: "POST", url: CREATE, body: makeChallengeCreationResponse({ id: "abc" }) },
      { url: TABLE, body: makeChallengeTableResponse({ creator: true }) },
    ]);

    renderWithClient(<ChallengeShare gameId="g1" round={finishedRound()} />);
    await waitFor(() => expect(screen.getByRole("button", { name: "Challenge a friend" })).toBeEnabled());
    await userEvent.click(screen.getByRole("button", { name: "Challenge a friend" }));

    expect(callsTo(fetchMock).filter((c) => c.url === CREATE)).toHaveLength(1);
    const text = writeText.mock.calls[0][0] as string;
    expect(text).toContain("can you beat my score?");
    expect(text).toContain("https://geoggernaut.com/challenge?id=abc");
    await waitFor(() => expect(screen.getByRole("button", { name: "Copied" })).toBeInTheDocument());
    expect(screen.queryByRole("button", { name: "Reply" })).toBeNull();
    expect(screen.queryByRole("button", { name: "Pass it on" })).toBeNull();
  });

  it("gives a challenger Reply and Pass it on, both with the original link", async () => {
    // Set up first: userEvent.setup() would otherwise put its own clipboard over the mock.
    const user = userEvent.setup();
    const writeText = vi.fn().mockResolvedValue(undefined);
    Object.defineProperty(navigator, "clipboard", { value: { writeText }, configurable: true });
    mockFetch([
      { method: "POST", url: CREATE, body: makeChallengeCreationResponse({ id: "abc" }) },
      { url: TABLE, body: makeChallengeTableResponse() },
    ]);

    renderWithClient(<ChallengeShare gameId="g1" round={finishedRound()} />);
    await user.click(await screen.findByRole("button", { name: "Reply" }));
    await user.click(screen.getByRole("button", { name: "Pass it on" }));

    const [reply, passOn] = writeText.mock.calls.map((call) => call[0] as string);
    expect(reply).toContain("6th of 6.");
    expect(reply).toContain("https://geoggernaut.com/challenge?id=abc");
    expect(passOn).toContain("The top score is 320 / ");
    expect(passOn).toContain("https://geoggernaut.com/challenge?id=abc");
    expect(screen.queryByRole("button", { name: "Challenge a friend" })).toBeNull();
  });

  it("keeps the single button when the table can't be loaded", async () => {
    mockFetch([
      { method: "POST", url: CREATE, body: makeChallengeCreationResponse() },
      { url: TABLE, status: 500, body: {} },
    ]);

    renderWithClient(<ChallengeShare gameId="g1" round={finishedRound()} />);

    await waitFor(() => expect(screen.getByRole("button", { name: "Challenge a friend" })).toBeEnabled());
  });

  it("is disabled while the challenge is being made", async () => {
    mockFetch([{ method: "POST", url: CREATE, body: () => new Promise(() => {}) }]);

    renderWithClient(<ChallengeShare gameId="g1" round={finishedRound()} />);

    expect(await screen.findByRole("button", { name: "Challenge a friend" })).toBeDisabled();
  });

  it("says so, with no button, when the challenge can't be made", async () => {
    mockFetch([{ method: "POST", url: CREATE, status: 500, body: {} }]);

    renderWithClient(<ChallengeShare gameId="g1" round={finishedRound()} />);

    expect(await screen.findByText("Couldn't make a challenge link")).toBeInTheDocument();
    expect(screen.queryByRole("button")).toBeNull();
  });
});
