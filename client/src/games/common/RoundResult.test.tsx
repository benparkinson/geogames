import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { makeRoundProps } from "../../test/fixtures";
import RoundResult from "./RoundResult";
import type { RoundView } from "./roundView";

function makeView(overrides: Partial<RoundView> = {}): RoundView {
  return {
    answerState: "CORRECT",
    points: 100,
    outcome: "firstGuess",
    result: "firstGuess",
    hintsUsed: 0,
    answersFound: 1,
    answerCount: 1,
    wrongGuesses: 0,
    hintTexts: [],
    continentText: null,
    ...overrides,
  };
}

describe("RoundResult", () => {
  it("shows a spinner on a disabled next button while saving, then focuses it", () => {
    const props = makeRoundProps({ answerSave: "saving" });
    const { rerender } = render(<RoundResult view={makeView()} round={props} />);

    expect(screen.getByRole("button")).toBeDisabled();
    expect(screen.getByRole("status")).toBeInTheDocument();

    rerender(<RoundResult view={makeView()} round={{ ...props, answerSave: "saved" }} />);

    expect(screen.getByRole("button", { name: /Next round/ })).toHaveFocus();
    expect(screen.queryByRole("status")).not.toBeInTheDocument();
  });

  it("celebrates a first guess", () => {
    render(<RoundResult view={makeView()} round={makeRoundProps()} />);

    expect(screen.getByText("First guess! +100")).toHaveAttribute("data-outcome", "firstGuess");
    expect(screen.queryByText(/hints? used/)).not.toBeInTheDocument();
    expect(screen.queryByText(/Answer revealed/)).not.toBeInTheDocument();
  });

  it("says no wrong guesses for a first guess in a game with several answers", () => {
    render(<RoundResult view={makeView({ answerCount: 3, answersFound: 3 })} round={makeRoundProps()} />);

    expect(screen.getByText("No wrong guesses! +100")).toBeInTheDocument();
  });

  it("calls a win after a wrong guess correct, not a first guess", () => {
    render(<RoundResult view={makeView({ outcome: "correct", result: "correct" })} round={makeRoundProps()} />);

    expect(screen.getByText("Correct! +100")).toHaveAttribute("data-outcome", "correct");
    expect(screen.queryByText(/First guess/)).not.toBeInTheDocument();
  });

  it("scores a correct answer with a hint, without a separate hints line", () => {
    const view = makeView({ outcome: "hints", result: "hints", hintsUsed: 1, points: 83 });
    render(<RoundResult view={view} round={makeRoundProps()} />);

    expect(screen.getByText("Correct with a hint · +83")).toHaveAttribute("data-outcome", "hints");
    expect(screen.queryByText(/hints? used/)).not.toBeInTheDocument();
  });

  it("pluralises the hints", () => {
    const view = makeView({ outcome: "hints", result: "hints", hintsUsed: 2, points: 67 });
    render(<RoundResult view={view} round={makeRoundProps()} />);

    expect(screen.getByText("Correct with 2 hints · +67")).toBeInTheDocument();
  });

  it("says the answer was revealed, for no points", () => {
    const view = makeView({ outcome: "missed", result: "revealed", points: 0, answersFound: 0, hintsUsed: 2 });
    render(<RoundResult view={view} round={makeRoundProps()} />);

    expect(screen.getByText("Answer revealed · 0 pts")).toHaveAttribute("data-outcome", "missed");
    expect(screen.queryByText(/Correct/)).not.toBeInTheDocument();
    expect(screen.queryByText(/hints? used/)).not.toBeInTheDocument();
  });

  it("says the guesses ran out, for no points", () => {
    const view = makeView({ outcome: "missed", result: "outOfGuesses", points: 0, answersFound: 0 });
    render(<RoundResult view={view} round={makeRoundProps()} />);

    expect(screen.getByText("Out of guesses · 0 pts")).toHaveAttribute("data-result", "outOfGuesses");
    expect(screen.queryByText(/Answer revealed/)).not.toBeInTheDocument();
  });

  it("says how many were found and the hints used for a round given up with some found", () => {
    const view = makeView({
      outcome: "hints",
      result: "revealed",
      points: 55,
      answersFound: 2,
      answerCount: 3,
      hintsUsed: 1,
    });
    render(<RoundResult view={view} round={makeRoundProps()} />);

    const banner = screen.getByText("Answer revealed · 2 of 3 found · +55");
    expect(banner).toHaveAttribute("data-outcome", "hints");
    expect(banner).toHaveAttribute("data-result", "revealed");
    expect(screen.getByText("1 hint used")).toBeInTheDocument();
  });

  it("says how many were found for a round that ran out of guesses", () => {
    const view = makeView({ outcome: "hints", result: "outOfGuesses", points: 50, answersFound: 1, answerCount: 2 });
    render(<RoundResult view={view} round={makeRoundProps()} />);

    expect(screen.getByText("Out of guesses · 1 of 2 found · +50")).toBeInTheDocument();
  });

  it("offers the next round when there is one", async () => {
    const round = makeRoundProps({ hasNextRound: true });
    render(<RoundResult view={makeView()} round={round} />);

    await userEvent.setup().click(screen.getByRole("button", { name: /Next round/ }));

    expect(round.nextRound).toHaveBeenCalledTimes(1);
    expect(screen.queryByRole("button", { name: "See results" })).not.toBeInTheDocument();
  });

  it("offers the results on the last round", async () => {
    const round = makeRoundProps({ hasNextRound: false });
    render(<RoundResult view={makeView()} round={round} />);

    await userEvent.setup().click(screen.getByRole("button", { name: "See results" }));

    expect(round.showSummary).toHaveBeenCalledTimes(1);
    expect(screen.queryByRole("button", { name: /Next round/ })).not.toBeInTheDocument();
  });
});
