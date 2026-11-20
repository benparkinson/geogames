import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { RoundOutcome } from "./outcome";
import RoundProgress from "./RoundProgress";

const states = ["CORRECT", "GAVE_UP", "UNANSWERED", "UNANSWERED"];
const points = [83, 0, 0, 0];
const outcomes: (RoundOutcome | null)[] = ["hints", "missed", null, null];

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

  it("labels each round by its status", () => {
    renderProgress();

    expect(screen.getByRole("button", { name: "Round 1: correct with hints, 83 points" })).toBeInTheDocument();
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

  it("shows a first guess as a green star", () => {
    renderOne("CORRECT", "firstGuess", 100);

    const dot = screen.getByRole("button", { name: "Round 1: first guess" });
    expect(dot).toHaveClass("round-dot--first-guess");
    expect(dot).not.toHaveClass("round-dot--correct");
    expect(dot.querySelector("svg.bi-star-fill")).not.toBeNull();
  });

  it("shows a correct round worth 100 points but not a first guess as a green check, not a star", () => {
    renderOne("CORRECT", "correct", 100);

    const dot = screen.getByRole("button", { name: "Round 1: correct" });
    expect(dot).toHaveClass("round-dot--correct");
    expect(dot).not.toHaveClass("round-dot--first-guess");
    expect(dot.querySelector("svg.bi-check-lg")).not.toBeNull();
    expect(dot.querySelector("svg.bi-star-fill")).toBeNull();
  });

  it("shows a correct round with hints as a yellow check", () => {
    renderOne("CORRECT", "hints", 67);

    const dot = screen.getByRole("button", { name: "Round 1: correct with hints, 67 points" });
    expect(dot).toHaveClass("round-dot--hints");
    expect(dot.querySelector("svg.bi-check-lg")).not.toBeNull();
  });

  it("shows a given up round with no points as a red eye", () => {
    renderOne("GAVE_UP", "missed", 0);

    const dot = screen.getByRole("button", { name: "Round 1: revealed" });
    expect(dot).toHaveClass("round-dot--missed");
    expect(dot.querySelector("svg.bi-eye-fill")).not.toBeNull();
  });

  it("shows a round that ran out of guesses as a red cross", () => {
    renderOne("OUT_OF_GUESSES", "missed", 0);

    const dot = screen.getByRole("button", { name: "Round 1: out of guesses" });
    expect(dot).toHaveClass("round-dot--missed");
    expect(dot.querySelector("svg.bi-x-lg")).not.toBeNull();
  });

  it("shows a revealed round with points as yellow, keeping its eye or cross", () => {
    render(
      <RoundProgress
        states={["GAVE_UP", "OUT_OF_GUESSES", "UNANSWERED"]}
        points={[67, 25, 0]}
        outcomes={["hints", "hints", null]}
        currentRound={2}
        goToRound={vi.fn()}
      />,
    );

    const gaveUp = screen.getByRole("button", { name: "Round 1: revealed, 67 points" });
    expect(gaveUp).toHaveClass("round-dot--hints");
    expect(gaveUp).not.toHaveClass("round-dot--missed");
    expect(gaveUp.querySelector("svg.bi-eye-fill")).not.toBeNull();
    const outOfGuesses = screen.getByRole("button", { name: "Round 2: out of guesses, 25 points" });
    expect(outOfGuesses).toHaveClass("round-dot--hints");
    expect(outOfGuesses.querySelector("svg.bi-x-lg")).not.toBeNull();
  });

  it("shows the number of an unanswered round, and the ring on the current one", () => {
    renderProgress(vi.fn(), 3);

    expect(screen.getByRole("button", { name: "Round 4: current" })).toHaveClass("round-dot--current");
    expect(screen.getByRole("button", { name: "Round 4: current" })).toHaveTextContent("4");
  });
});
