import { render, screen, within } from "@testing-library/react";
import { beforeEach, describe, expect, it } from "vitest";
import { resetDistanceUnitForTests } from "../../settings/units";
import GuessList from "./GuessList";
import type { Guess, GuessState } from "./guessReducer";

function stateOf(guesses: Guess[], overrides: Partial<GuessState> = {}): GuessState {
  return { guesses, gaveUp: false, outOfGuesses: false, over: false, hintsUsed: 0, foundAtHint: [], ...overrides };
}

function renderList(state: GuessState, answerCount = 3, allAnswers: string[] = [], showClues = true) {
  return render(<GuessList state={state} answerCount={answerCount} allAnswers={allAnswers} showClues={showClues} />);
}

beforeEach(() => {
  resetDistanceUnitForTests();
  localStorage.clear();
});

describe("GuessList", () => {
  it("scrolls to the newest guess when one is added", () => {
    const one = stateOf([{ text: "Peru", answer: null }]);
    const { rerender } = renderList(one);
    const list = screen.getByRole("list");
    Object.defineProperty(list, "scrollHeight", { configurable: true, value: 300 });

    const two = stateOf([...one.guesses, { text: "Chile", answer: null }]);
    rerender(<GuessList state={two} answerCount={3} allAnswers={[]} showClues />);

    expect(list.scrollTop).toBe(300);
  });

  it("counts the answers found", () => {
    renderList(stateOf([{ text: "france", answer: "France" }]));

    expect(screen.getByRole("group", { name: "Answers: 1 of 3 found" })).toBeInTheDocument();
    expect(screen.getByText("1/3 found")).toBeInTheDocument();
    expect(screen.queryByRole("heading")).toBeNull();
  });

  it("shows no count for a single answer", () => {
    renderList(stateOf([{ text: "Peru", answer: null }]), 1);

    expect(screen.queryByText(/found$/)).toBeNull();
    expect(screen.getAllByRole("listitem")).toHaveLength(1);
  });

  it("lists the rows in guess order, without numbers", () => {
    renderList(
      stateOf([
        { text: "Peru", answer: null },
        { text: "france", answer: "France" },
      ]),
    );

    const rows = screen.getAllByRole("listitem");
    expect(rows).toHaveLength(2);
    expect(rows[0]).toHaveTextContent("Peru");
    expect(rows[1]).toHaveTextContent("France found");
  });

  it("shows only the count before the first guess of a multi-answer round", () => {
    renderList(stateOf([]));

    expect(screen.getByText("0/3 found")).toBeInTheDocument();
    expect(screen.queryByRole("listitem")).toBeNull();
  });

  it("shows a wrong guess's distance and a hidden direction, unless clues are off", () => {
    const wrong = [{ text: "France", answer: null, clue: { km: 1236, bearing: 90 } }];
    const view = renderList(stateOf(wrong));

    expect(screen.getByTestId("wrong-guess")).toHaveTextContent(/1,240 km/);
    expect(screen.getByText("1,240 km east")).toBeInTheDocument();

    view.unmount();
    renderList(stateOf(wrong), 3, [], false);
    expect(screen.getByTestId("wrong-guess")).not.toHaveTextContent(/km/);
  });

  it("shows the distance in miles when chosen", () => {
    localStorage.setItem("gg-units", "mi");
    renderList(stateOf([{ text: "France", answer: null, clue: { km: 1236, bearing: 90 } }]));

    expect(screen.getByTestId("wrong-guess")).toHaveTextContent(/770 mi/);
    expect(screen.getByText("770 mi east")).toBeInTheDocument();
    expect(screen.getByTestId("wrong-guess")).not.toHaveTextContent(/km/);
  });

  it("shows the answers that weren't found as missed once revealed", () => {
    renderList(stateOf([{ text: "italy", answer: "Italy" }], { gaveUp: true, over: true }), 3, [
      "France",
      "Spain",
      "Italy",
    ]);

    const missed = screen.getAllByRole("listitem").filter((li) => li.getAttribute("data-outcome") === "missed");
    expect(missed.map((li) => within(li).getByText(/France|Spain/).textContent)).toEqual(["France", "Spain"]);
  });

  it("takes no room for a round with one answer until a guess is made", () => {
    const { container } = renderList(stateOf([]), 1, ["France"]);

    expect(container).toBeEmptyDOMElement();
  });

  describe("found animation", () => {
    const found: Guess = { text: "france", answer: "France" };
    const wrong: Guess = { text: "Peru", answer: null };

    it("does not mark a found row present on mount", () => {
      renderList(stateOf([found]));

      expect(screen.getAllByRole("listitem")[0]).not.toHaveAttribute("data-fresh");
    });

    it("marks only a found row added after mount", () => {
      const { rerender } = renderList(stateOf([found]));
      const next = stateOf([found, { text: "spain", answer: "Spain" }]);
      rerender(<GuessList state={next} answerCount={3} allAnswers={[]} showClues />);

      const rows = screen.getAllByRole("listitem");
      expect(rows[0]).not.toHaveAttribute("data-fresh");
      expect(rows[1]).toHaveAttribute("data-fresh", "true");
    });

    it("does not mark a wrong guess added after mount", () => {
      const { rerender } = renderList(stateOf([found]));
      rerender(<GuessList state={stateOf([found, wrong])} answerCount={3} allAnswers={[]} showClues />);

      expect(screen.getAllByRole("listitem")[1]).not.toHaveAttribute("data-fresh");
    });
  });
});
