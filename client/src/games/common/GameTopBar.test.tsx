import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { makeRoundProps } from "../../test/fixtures";
import GameTopBar from "./GameTopBar";

describe("GameTopBar", () => {
  it("shows the running score mid-game", () => {
    render(<GameTopBar round={makeRoundProps({ score: 183 })} gameOver={false} gaveUp={false} hintsUsed={0} />);

    expect(screen.getByText("Score 183")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Results" })).not.toBeInTheDocument();
  });

  it("shows the final score out of the maximum and a results button when the game is complete", async () => {
    const round = makeRoundProps({
      score: 183,
      maxScore: 300,
      totalRoundCount: 3,
      gameCompleted: true,
      answerState: "CORRECT",
    });
    render(<GameTopBar round={round} gameOver={true} gaveUp={false} hintsUsed={1} />);

    expect(screen.getByText("Final score 183 / 300")).toBeInTheDocument();
    await userEvent.setup().click(screen.getByRole("button", { name: "Results" }));
    expect(round.showSummary).toHaveBeenCalledTimes(1);
  });

  it("adds a round just finished here to the score before the server's copy is refetched", () => {
    render(<GameTopBar round={makeRoundProps({ score: 100 })} gameOver={true} gaveUp={false} hintsUsed={1} />);

    // A tripoint round (3 hints) answered with one hint is worth 83.
    expect(screen.getByText("Score 183")).toBeInTheDocument();
  });

  it("adds nothing for a round revealed here", () => {
    render(<GameTopBar round={makeRoundProps({ score: 100 })} gameOver={true} gaveUp={true} hintsUsed={2} />);

    expect(screen.getByText("Score 100")).toBeInTheDocument();
  });

  it("does not count an already answered round twice", () => {
    const round = makeRoundProps({ score: 183, answerState: "CORRECT", hintsUsed: 1, points: 83 });
    render(<GameTopBar round={round} gameOver={true} gaveUp={false} hintsUsed={1} />);

    expect(screen.getByText("Score 183")).toBeInTheDocument();
  });

  it("shows which round is being played", () => {
    render(<GameTopBar round={makeRoundProps({ currentRound: 1, totalRoundCount: 3 })} gameOver={false} gaveUp={false} hintsUsed={0} />);

    expect(screen.getByText("Round 2 of 3")).toBeInTheDocument();
  });

  it("links home", () => {
    render(<GameTopBar round={makeRoundProps()} gameOver={false} gaveUp={false} hintsUsed={0} />);

    expect(screen.getByRole("link", { name: "Home" })).toHaveAttribute("href", "/");
  });

  it("shows the current round as perfect straight away when the player has just won without hints", () => {
    render(<GameTopBar round={makeRoundProps()} gameOver={true} gaveUp={false} hintsUsed={0} />);

    expect(screen.getByRole("button", { name: "Round 1: current" })).toHaveClass("round-dot--perfect");
  });

  it("shows the current round as correct straight away when the player won with a hint", () => {
    render(<GameTopBar round={makeRoundProps()} gameOver={true} gaveUp={false} hintsUsed={1} />);

    const dot = screen.getByRole("button", { name: "Round 1: current" });
    expect(dot).toHaveClass("round-dot--correct");
    expect(dot).not.toHaveClass("round-dot--perfect");
  });

  it("shows the current round as revealed straight away when the player gave up", () => {
    render(<GameTopBar round={makeRoundProps()} gameOver={true} gaveUp={true} hintsUsed={0} />);

    expect(screen.getByRole("button", { name: "Round 1: current" })).toHaveClass("round-dot--revealed");
  });

  it("leaves the current round unmarked while it is still being played", () => {
    render(<GameTopBar round={makeRoundProps()} gameOver={false} gaveUp={false} hintsUsed={0} />);

    const dot = screen.getByRole("button", { name: "Round 1: current" });
    expect(dot).not.toHaveClass("round-dot--correct");
    expect(dot).not.toHaveClass("round-dot--perfect");
    expect(dot).not.toHaveClass("round-dot--revealed");
  });

  it("does not mutate the round it was given", () => {
    const round = makeRoundProps();
    const states = [...round.roundAnswerStates];
    const points = [...round.roundPoints];

    render(<GameTopBar round={round} gameOver={true} gaveUp={true} hintsUsed={0} />);

    expect(round.roundAnswerStates).toEqual(states);
    expect(round.roundPoints).toEqual(points);
  });
});
