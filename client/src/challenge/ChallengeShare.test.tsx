import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { callsTo, mockFetch } from "../test/fetchMock";
import { makeChallengeCreationResponse, makeGameRounds } from "../test/fixtures";
import { renderWithClient } from "../test/render";
import { toRoundView } from "../api/games";
import ChallengeShare from "./ChallengeShare";

const CREATE = "http://localhost:8080/api/games/g1/challenges";

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
    const fetchMock = mockFetch([{ method: "POST", url: CREATE, body: makeChallengeCreationResponse({ id: "abc" }) }]);

    renderWithClient(<ChallengeShare gameId="g1" round={finishedRound()} />);
    await waitFor(() => expect(screen.getByRole("button", { name: "Challenge a friend" })).toBeEnabled());
    await userEvent.click(screen.getByRole("button", { name: "Challenge a friend" }));

    expect(callsTo(fetchMock).filter((c) => c.url === CREATE)).toHaveLength(1);
    const text = writeText.mock.calls[0][0] as string;
    expect(text).toContain("can you beat my score?");
    expect(text).toContain("https://geoggernaut.com/challenge?id=abc");
    await waitFor(() => expect(screen.getByRole("button", { name: "Copied" })).toBeInTheDocument());
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
