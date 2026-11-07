import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import RoundProgress from "./RoundProgress";

const states = ["CORRECT", "GAVE_UP", "UNANSWERED", "UNANSWERED"];

function renderProgress(goToRound = vi.fn(), currentRound = 2) {
  render(<RoundProgress states={states} currentRound={currentRound} goToRound={goToRound} />);
  return goToRound;
}

describe("RoundProgress", () => {
  it("renders one button per round", () => {
    renderProgress();

    expect(screen.getAllByRole("button")).toHaveLength(4);
  });

  it("labels each round by its status", () => {
    renderProgress();

    expect(screen.getByRole("button", { name: "Round 1: correct" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Round 2: revealed" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Round 3: current" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Round 4: not played" })).toBeInTheDocument();
  });

  it("marks only the current round with aria-current", () => {
    renderProgress();

    const current = screen.getAllByRole("button").filter((b) => b.getAttribute("aria-current") === "step");
    expect(current).toHaveLength(1);
    expect(current[0]).toHaveAccessibleName("Round 3: current");
  });

  it("enables answered rounds, the current round and the first unanswered round only", () => {
    renderProgress(vi.fn(), 0);

    // currentRound 0 is answered; round 3 (index 2) is the first unanswered; index 3 is locked.
    expect(screen.getByRole("button", { name: "Round 1: current" })).toBeEnabled();
    expect(screen.getByRole("button", { name: "Round 2: revealed" })).toBeEnabled();
    expect(screen.getByRole("button", { name: "Round 3: not played" })).toBeEnabled();
    expect(screen.getByRole("button", { name: "Round 4: not played" })).toBeDisabled();
  });

  it("explains why a later round is locked", () => {
    renderProgress();

    expect(screen.getByTitle("Finish round 3 first")).toBeInTheDocument();
  });

  it("goes to the clicked round", async () => {
    const goToRound = renderProgress();

    await userEvent.setup().click(screen.getByRole("button", { name: "Round 2: revealed" }));

    expect(goToRound).toHaveBeenCalledWith(1);
  });

  it("does not navigate to a locked round", async () => {
    const goToRound = renderProgress();

    await userEvent.setup().click(screen.getByRole("button", { name: "Round 4: not played" }));

    expect(goToRound).not.toHaveBeenCalled();
  });

  it("styles correct and revealed rounds", () => {
    renderProgress(vi.fn(), 3);

    expect(screen.getByRole("button", { name: "Round 1: correct" })).toHaveClass("round-dot--correct");
    expect(screen.getByRole("button", { name: "Round 2: revealed" })).toHaveClass("round-dot--revealed");
    expect(screen.getByRole("button", { name: "Round 4: current" })).toHaveClass("round-dot--current");
  });
});
