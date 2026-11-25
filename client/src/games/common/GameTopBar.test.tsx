import { render, screen, within } from "@testing-library/react";
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

  it("shows the running score over its label mid-game", () => {
    renderBar({}, { score: 183 });

    const score = screen.getByRole("group", { name: "Score 183" });
    expect(within(score).getByText("183")).toBeInTheDocument();
    expect(within(score).getByText("score")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Results" })).not.toBeInTheDocument();
  });

  it("shows the final score out of the maximum over a results button when the game is complete", async () => {
    const round = renderBar({ maxScore: 300, totalRoundCount: 3, gameCompleted: true }, { score: 183 });

    const score = screen.getByRole("group", { name: "Final score 183 of 300" });
    expect(within(score).getByText("183 / 300")).toBeInTheDocument();
    await userEvent.setup().click(within(score).getByRole("button", { name: "Results" }));
    expect(round.showSummary).toHaveBeenCalledTimes(1);
  });

  it("disables the results button until the answers are saved", () => {
    renderBar({ gameCompleted: true, answerSave: "saving" });

    expect(screen.getByRole("button", { name: "Results" })).toBeDisabled();
  });

  it("names the round instead of numbering it: the round dots show the number", () => {
    renderBar({ currentRound: 1, totalRoundCount: 3 });

    expect(screen.getByText("Test Game")).toBeInTheDocument();
    expect(screen.queryByText(/round 0/)).not.toBeInTheDocument();
  });

  it("calls an ordinary game Free play, and shows the difficulty only when it is not Normal", () => {
    const { unmount } = render(
      <GameTopBar round={makeRoundProps({ difficulty: "HARD" })} progress={progressOf(makeRoundProps())} />,
    );
    expect(screen.getByText("Free play")).toBeInTheDocument();
    expect(screen.getByText("Test Game").parentElement).toHaveTextContent(/^Test Game · Hard mode$/);
    unmount();

    const easy = render(
      <GameTopBar round={makeRoundProps({ difficulty: "EASY" })} progress={progressOf(makeRoundProps())} />,
    );
    expect(screen.getByText("Test Game").parentElement).toHaveTextContent(/^Test Game · Easy$/);
    easy.unmount();

    renderBar();
    expect(screen.getByText("Test Game").parentElement).toHaveTextContent(/^Test Game$/);
  });

  it("links home", () => {
    renderBar();

    expect(screen.getByRole("link", { name: "Home" })).toHaveAttribute("href", "/");
  });

  it("shows the site icon and name as the home link, like every other header", () => {
    renderBar();

    const home = screen.getByRole("link", { name: "Home" });
    expect(home).toHaveTextContent("GeoGauntlet");
    expect(home.querySelector("img")).toHaveAttribute("src", "/logo.png");
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
