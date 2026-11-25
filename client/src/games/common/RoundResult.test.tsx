import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { makeRoundProps } from "../../test/fixtures";
import RoundResult from "./RoundResult";
import type { RoundView } from "./roundView";
import { pointsBreakdown } from "./scoring";

function makeView(overrides: Partial<RoundView> = {}): RoundView {
  return {
    answerState: "CORRECT",
    points: 10,
    breakdown: { answer: 4, noHints: 3, noMisses: 3 },
    outcome: "full",
    result: "correct",
    hintsUsed: 0,
    answersFound: 1,
    answerCount: 1,
    wrongGuesses: 0,
    hintTexts: [],
    continentText: null,
    ...overrides,
  };
}

/** The banner is the live status: its outcome, the title and the points. */
function expectBanner({
  outcome,
  result,
  label,
  points,
}: {
  outcome: string;
  result?: string;
  label: string;
  points: string;
}): void {
  const banner = screen.getByRole("status");
  expect(banner).toHaveAttribute("data-outcome", outcome);
  if (result === undefined) {
    expect(banner).not.toHaveAttribute("data-result");
  } else {
    expect(banner).toHaveAttribute("data-result", result);
  }
  expect(banner).toHaveTextContent(`${label} ${points}`);
}

const cells = (container: HTMLElement) => Array.from(container.querySelectorAll<HTMLElement>("[data-part]"));

describe("RoundResult", () => {
  it("shows a spinner on a disabled next button while saving, then focuses it", () => {
    const props = makeRoundProps({ answerSave: "saving" });
    const { rerender } = render(<RoundResult view={makeView()} round={props} />);

    const button = screen.getByRole("button");
    expect(button).toBeDisabled();
    expect(within(button).getByRole("status")).toBeInTheDocument();

    rerender(<RoundResult view={makeView()} round={{ ...props, answerSave: "saved" }} />);

    expect(screen.getByRole("button", { name: /Next round/ })).toHaveFocus();
    expect(within(screen.getByRole("button")).queryByRole("status")).not.toBeInTheDocument();
  });

  it("shows the title, the points and a bar with the three named parts", () => {
    const view = makeView({
      points: 9,
      outcome: "good",
      hintsUsed: 1,
      breakdown: pointsBreakdown("CORRECT", 1, 1, 1, 0),
    });
    render(<RoundResult view={view} round={makeRoundProps()} />);

    expectBanner({ outcome: "good", label: "Correct!", points: "+9" });
    expect(screen.getByRole("img", { name: "9 of 10: answer 4, hints 2 of 3, accuracy 3 of 3" })).toBeInTheDocument();
    expect(screen.getByText("Answer 4/4")).toBeInTheDocument();
    expect(screen.getByText("Hints 2/3")).toBeInTheDocument();
    expect(screen.getByText("Accuracy 3/3")).toBeInTheDocument();
  });

  it("says the answer was revealed, for no points", () => {
    const view = makeView({
      outcome: "missed",
      result: "revealed",
      points: 0,
      answersFound: 0,
      hintsUsed: 2,
      breakdown: { answer: 0, noHints: 0, noMisses: 0 },
    });
    render(<RoundResult view={view} round={makeRoundProps()} />);

    expectBanner({ outcome: "missed", result: "revealed", label: "Answer revealed", points: "+0" });
    expect(screen.queryByText(/Correct/)).not.toBeInTheDocument();
  });

  it("says the guesses ran out, for no points", () => {
    const view = makeView({
      outcome: "missed",
      result: "outOfGuesses",
      points: 0,
      answersFound: 0,
      breakdown: { answer: 0, noHints: 0, noMisses: 0 },
    });
    render(<RoundResult view={view} round={makeRoundProps()} />);

    expectBanner({ outcome: "missed", result: "outOfGuesses", label: "Out of guesses", points: "+0" });
    expect(screen.queryByText(/Answer revealed/)).not.toBeInTheDocument();
  });

  it("says how many were found for a round given up with some found", () => {
    const view = makeView({
      outcome: "partial",
      result: "revealed",
      points: 5,
      answersFound: 2,
      answerCount: 3,
      breakdown: pointsBreakdown("GAVE_UP", 0, 2, 3, 0),
    });
    render(<RoundResult view={view} round={makeRoundProps()} />);

    expectBanner({ outcome: "partial", result: "revealed", label: "Answer revealed", points: "+5" });
    expect(screen.getByText("Found 2 of 3")).toBeInTheDocument();
  });

  it("fills the bar cell by cell for a round that has just ended, with every cell still present", () => {
    const { container } = render(<RoundResult view={makeView()} round={makeRoundProps()} animate />);

    expect(container.querySelector("[data-animate]")).not.toBeNull();
    expect(cells(container)).toHaveLength(10);
    expect(cells(container).every((c) => c.dataset.earned === "true")).toBe(true);
  });

  it("shows a revisited round's bar already filled, with no animation", () => {
    const { container } = render(<RoundResult view={makeView()} round={makeRoundProps()} />);

    expect(container.querySelector("[data-animate]")).toBeNull();
    expect(cells(container)).toHaveLength(10);
  });

  it("focuses the next round button at once, while the bar fills", () => {
    render(<RoundResult view={makeView()} round={makeRoundProps({ hasNextRound: true })} animate />);

    expect(screen.getByRole("button", { name: /Next round/ })).toHaveFocus();
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
