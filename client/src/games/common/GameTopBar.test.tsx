import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { makeRoundProps } from "../../test/fixtures";
import GameTopBar from "./GameTopBar";
import type { LiveProgress } from "./roundView";

type TestRound = ReturnType<typeof makeRoundProps>;

function progressOf(round: TestRound, overrides: Partial<LiveProgress> = {}): LiveProgress {
  return {
    states: round.roundAnswerStates,
    points: round.roundPoints,
    outcomes: round.roundAnswerStates.map(() => null),
    score: round.score,
    ...overrides,
  };
}

function renderBar(overrides: Partial<TestRound> = {}, progress: Partial<LiveProgress> = {}): TestRound {
  const round = makeRoundProps(overrides);
  render(<GameTopBar round={round} progress={progressOf(round, progress)} />);
  return round;
}

describe("GameTopBar", () => {
  it("names a daily challenge", () => {
    renderBar({ dailyDate: "2026-11-18" });

    expect(screen.getByText("Daily")).toBeInTheDocument();
  });

  it("says nothing about a daily in an ordinary game", () => {
    renderBar();

    expect(screen.queryByText(/Daily/)).not.toBeInTheDocument();
  });

  it("has no theme button", () => {
    renderBar();

    expect(screen.queryByRole("button", { name: /dark mode|light mode/ })).not.toBeInTheDocument();
  });

  it("shows the running score mid-game", () => {
    renderBar({}, { score: 183 });

    expect(screen.getByText("Score 183")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Results" })).not.toBeInTheDocument();
  });

  it("shows the final score out of the maximum and a results button when the game is complete", async () => {
    const round = renderBar({ maxScore: 300, totalRoundCount: 3, gameCompleted: true }, { score: 183 });

    expect(screen.getByText("Final score 183 / 300")).toBeInTheDocument();
    await userEvent.setup().click(screen.getByRole("button", { name: "Results" }));
    expect(round.showSummary).toHaveBeenCalledTimes(1);
  });

  it("shows which round is being played", () => {
    renderBar({ currentRound: 1, totalRoundCount: 3 });

    expect(screen.getByText("round 02 of 03")).toBeInTheDocument();
  });

  it("calls an ordinary game Practice, and shows the difficulty only when it is not Normal", () => {
    const { unmount } = render(
      <GameTopBar round={makeRoundProps({ difficulty: "HARD" })} progress={progressOf(makeRoundProps())} />,
    );
    expect(screen.getByText("Practice")).toBeInTheDocument();
    expect(screen.queryByText("Test Game")).not.toBeInTheDocument();
    expect(screen.getByText("round 01 of 03 · Hard mode")).toBeInTheDocument();
    unmount();

    renderBar();
    expect(screen.getByText(/^round 01 of \d+$/)).toBeInTheDocument();
  });

  it("links home", () => {
    renderBar();

    expect(screen.getByRole("link", { name: "Home" })).toHaveAttribute("href", "/");
  });

  it("draws each round's outcome from the progress it is given", () => {
    const round = makeRoundProps();
    const outcomes = round.roundAnswerStates.map(() => null) as LiveProgress["outcomes"];
    outcomes[round.currentRound] = "missed";
    render(<GameTopBar round={round} progress={progressOf(round, { outcomes })} />);

    expect(screen.getByRole("button", { name: "Round 1: current" })).toHaveAttribute("data-outcome", "missed");
  });

  it("leaves the current round unmarked while it is still being played", () => {
    renderBar();

    expect(screen.getByRole("button", { name: "Round 1: current" })).not.toHaveAttribute("data-outcome");
  });
});
