import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { makeRoundProps } from "../../test/fixtures";
import GameTopBar from "./GameTopBar";

describe("GameTopBar", () => {
  it("shows the running score mid-game", () => {
    render(<GameTopBar round={makeRoundProps({ score: 2 })} gameOver={false} gaveUp={false} />);

    expect(screen.getByText("Score 2")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Results" })).not.toBeInTheDocument();
  });

  it("shows the final score and a results button when the game is complete", async () => {
    const round = makeRoundProps({ score: 2, totalRoundCount: 3, gameCompleted: true });
    render(<GameTopBar round={round} gameOver={true} gaveUp={false} />);

    expect(screen.getByText("Final score 2 / 3")).toBeInTheDocument();
    await userEvent.setup().click(screen.getByRole("button", { name: "Results" }));
    expect(round.showSummary).toHaveBeenCalledTimes(1);
  });

  it("shows which round is being played", () => {
    render(<GameTopBar round={makeRoundProps({ currentRound: 1, totalRoundCount: 3 })} gameOver={false} gaveUp={false} />);

    expect(screen.getByText("Round 2 of 3")).toBeInTheDocument();
  });

  it("links home", () => {
    render(<GameTopBar round={makeRoundProps()} gameOver={false} gaveUp={false} />);

    expect(screen.getByRole("link", { name: "Home" })).toHaveAttribute("href", "/");
  });

  it("shows the current round as correct straight away when the player has just won", () => {
    render(<GameTopBar round={makeRoundProps()} gameOver={true} gaveUp={false} />);

    expect(screen.getByRole("button", { name: "Round 1: current" })).toHaveClass("round-dot--correct");
  });

  it("shows the current round as revealed straight away when the player gave up", () => {
    render(<GameTopBar round={makeRoundProps()} gameOver={true} gaveUp={true} />);

    expect(screen.getByRole("button", { name: "Round 1: current" })).toHaveClass("round-dot--revealed");
  });

  it("leaves the current round unmarked while it is still being played", () => {
    render(<GameTopBar round={makeRoundProps()} gameOver={false} gaveUp={false} />);

    const dot = screen.getByRole("button", { name: "Round 1: current" });
    expect(dot).not.toHaveClass("round-dot--correct");
    expect(dot).not.toHaveClass("round-dot--revealed");
  });

  it("does not mutate the round states it was given", () => {
    const round = makeRoundProps();
    const before = [...round.roundAnswerStates];

    render(<GameTopBar round={round} gameOver={true} gaveUp={true} />);

    expect(round.roundAnswerStates).toEqual(before);
  });
});
