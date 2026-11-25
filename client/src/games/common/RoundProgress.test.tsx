import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { RoundOutcome } from "./outcome";
import RoundProgress from "./RoundProgress";

const states = ["CORRECT", "GAVE_UP", "UNANSWERED", "UNANSWERED"];
const points = [9, 0, 0, 0];
const outcomes: (RoundOutcome | null)[] = ["good", "missed", null, null];

function renderProgress(goToRound = vi.fn(), currentRound = 2) {
  render(
    <RoundProgress
      states={states}
      points={points}
      outcomes={outcomes}
      currentRound={currentRound}
      goToRound={goToRound}
    />,
  );
  return goToRound;
}

function renderOne(state: string, outcome: RoundOutcome, pts: number) {
  render(
    <RoundProgress
      states={[state, "UNANSWERED"]}
      points={[pts, 0]}
      outcomes={[outcome, null]}
      currentRound={1}
      goToRound={vi.fn()}
    />,
  );
}

describe("RoundProgress", () => {
  it("renders one button per round", () => {
    renderProgress();

    expect(screen.getAllByRole("button")).toHaveLength(4);
  });

  it("labels each round by its points", () => {
    renderProgress();

    expect(screen.getByRole("button", { name: "Round 1: 9 points" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Round 2: 0 points" })).toBeInTheDocument();
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
    expect(screen.getByRole("button", { name: "Round 2: 0 points" })).toBeEnabled();
    expect(screen.getByRole("button", { name: "Round 3: not played" })).toBeEnabled();
    expect(screen.getByRole("button", { name: "Round 4: not played" })).toBeDisabled();
  });

  it("explains why a later round is locked", () => {
    renderProgress();

    expect(screen.getByTitle("Finish round 3 first")).toBeInTheDocument();
  });

  it("goes to the clicked round", async () => {
    const goToRound = renderProgress();

    await userEvent.setup().click(screen.getByRole("button", { name: "Round 2: 0 points" }));

    expect(goToRound).toHaveBeenCalledWith(1);
  });

  it("does not navigate to a locked round", async () => {
    const goToRound = renderProgress();

    await userEvent.setup().click(screen.getByRole("button", { name: "Round 4: not played" }));

    expect(goToRound).not.toHaveBeenCalled();
  });

  it.each([
    ["CORRECT", "full", 10],
    ["CORRECT", "good", 8],
    ["GAVE_UP", "partial", 5],
    ["OUT_OF_GUESSES", "missed", 0],
  ] as const)("shows a %s round's score on its dot, coloured by its %s outcome", (state, outcome, pts) => {
    renderOne(state, outcome, pts);

    const dot = screen.getByRole("button", { name: `Round 1: ${pts} points` });
    expect(dot).toHaveAttribute("data-outcome", outcome);
    expect(dot).toHaveTextContent(String(pts));
    expect(dot.querySelector("svg")).toBeNull();
  });

  it("says '1 point' in the singular", () => {
    renderOne("GAVE_UP", "partial", 1);

    expect(screen.getByRole("button", { name: "Round 1: 1 point" })).toBeInTheDocument();
  });

  it("shows the number of an unanswered round, with no outcome, and marks the current one", () => {
    renderProgress(vi.fn(), 3);

    const current = screen.getByRole("button", { name: "Round 4: current" });
    expect(current).toHaveAttribute("aria-current", "step");
    expect(current).toHaveTextContent("4");
    expect(current).not.toHaveAttribute("data-outcome");
  });
});
