import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { pointsBreakdown } from "../games/common/scoring";
import { ScoreBar, ScoreBarHeader, ScoreKey } from "./ScoreBar";

function cells(container: HTMLElement): HTMLElement[] {
  return Array.from(container.querySelectorAll<HTMLElement>("[data-part]"));
}

function earnedByPart(container: HTMLElement): Record<string, number> {
  const earned: Record<string, number> = { answer: 0, noHints: 0, noMisses: 0 };
  for (const cell of cells(container)) {
    if (cell.dataset.earned === "true") earned[cell.dataset.part as string]++;
  }
  return earned;
}

describe("ScoreBar", () => {
  it("always has 10 cells: 4 for the answer, 3 for hints, 3 for lives", () => {
    const { container } = render(<ScoreBar breakdown={{ answer: 0, noHints: 0, noMisses: 0 }} />);

    expect(cells(container).map((c) => c.dataset.part)).toEqual([
      ...Array(4).fill("answer"),
      ...Array(3).fill("noHints"),
      ...Array(3).fill("noMisses"),
    ]);
  });

  it.each([
    [
      "CORRECT",
      0,
      0,
      1,
      1,
      { answer: 4, noHints: 3, noMisses: 3 },
      "10 of 10: answer 4 of 4, hints 3 of 3, lives 3 of 3",
    ],
    [
      "CORRECT",
      1,
      1,
      1,
      1,
      { answer: 4, noHints: 2, noMisses: 2 },
      "8 of 10: answer 4 of 4, hints 2 of 3, lives 2 of 3",
    ],
    [
      "CORRECT",
      3,
      3,
      1,
      1,
      { answer: 4, noHints: 0, noMisses: 0 },
      "4 of 10: answer 4 of 4, hints 0 of 3, lives 0 of 3",
    ],
  ] as const)("fills the earned cells of a %s round", (state, hints, wrong, found, count, parts, text) => {
    const { container } = render(<ScoreBar breakdown={pointsBreakdown(state, hints, found, count, wrong)} />);

    expect(earnedByPart(container)).toEqual(parts);
    expect(screen.getByRole("img", { name: text })).toBeInTheDocument();
  });

  it("fills the Answer cells first for a revealed round, and says how many were found", () => {
    const { container } = render(<ScoreBar breakdown={pointsBreakdown("GAVE_UP", 0, 2, 3, 0)} showLabels />);

    expect(earnedByPart(container)).toEqual({ answer: 4, noHints: 1, noMisses: 0 });
    expect(screen.getByRole("img", { name: /5 of 10.*Found 2 of 3/ })).toBeInTheDocument();
    expect(screen.getByText("Found 2 of 3")).toBeInTheDocument();
  });

  it("is empty for no points", () => {
    const { container } = render(<ScoreBar breakdown={pointsBreakdown("GAVE_UP", 0, 0, 3, 0)} />);

    expect(earnedByPart(container)).toEqual({ answer: 0, noHints: 0, noMisses: 0 });
    expect(screen.getByRole("img", { name: "0 of 10: answer 0 of 4, hints 0 of 3, lives 0 of 3" })).toBeInTheDocument();
  });

  it("names the parts underneath only when asked", () => {
    const { rerender } = render(<ScoreBar breakdown={{ answer: 4, noHints: 2, noMisses: 3 }} />);
    expect(screen.queryByText("Hints +2")).not.toBeInTheDocument();

    rerender(<ScoreBar breakdown={{ answer: 4, noHints: 2, noMisses: 3 }} showLabels />);
    expect(screen.getByText("Answer +4")).toBeInTheDocument();
    expect(screen.getByText("Hints +2")).toBeInTheDocument();
    expect(screen.getByText("Lives +3")).toBeInTheDocument();
  });

  it("keeps the Hints name with no hint taken, and scores a wrong guess under Lives", () => {
    render(<ScoreBar breakdown={pointsBreakdown("CORRECT", 0, 1, 1, 1)} showLabels />);

    expect(screen.getByText("Hints +3")).toBeInTheDocument();
    expect(screen.getByText("Lives +2")).toBeInTheDocument();
    expect(screen.getByRole("img", { name: "9 of 10: answer 4 of 4, hints 3 of 3, lives 2 of 3" })).toBeInTheDocument();
  });

  it("names the three parts in the colour key", () => {
    render(<ScoreKey />);

    expect(screen.getAllByRole("listitem").map((item) => item.textContent)).toEqual(["Answer", "Hints", "Lives"]);
  });

  it("hides the column headers from screen readers", () => {
    const { container } = render(<ScoreBarHeader />);

    expect(container.firstElementChild).toHaveAttribute("aria-hidden", "true");
    expect(container).toHaveTextContent("AnswerHintsLives");
  });

  it("with animate keeps the same cells, flags the bar and numbers the earned cells in order", () => {
    const breakdown = { answer: 4, noHints: 1, noMisses: 0 };
    const plain = render(<ScoreBar breakdown={breakdown} />);
    const plainEarned = cells(plain.container).map((c) => c.dataset.earned);
    plain.unmount();

    const { container } = render(<ScoreBar breakdown={breakdown} animate />);

    expect(container.querySelector("[data-animate]")).not.toBeNull();
    expect(cells(container).map((c) => c.dataset.earned)).toEqual(plainEarned);
    const order = cells(container)
      .filter((c) => c.dataset.earned === "true")
      .map((c) => c.style.getPropertyValue("--i"));
    expect(order).toEqual(["0", "1", "2", "3", "4"]);
    expect(cells(container).filter((c) => c.dataset.earned === "false" && c.style.getPropertyValue("--i"))).toEqual([]);
  });
});
