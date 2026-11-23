import { render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import GuessList from "./GuessList";
import type { Guess, GuessState } from "./guessReducer";

function stateOf(guesses: Guess[], overrides: Partial<GuessState> = {}): GuessState {
  return { guesses, gaveUp: false, outOfGuesses: false, over: false, hintsUsed: 0, foundAtHint: [], ...overrides };
}

function renderList(state: GuessState, answerCount = 3, allAnswers: string[] = [], showClues = true) {
  return render(<GuessList state={state} answerCount={answerCount} allAnswers={allAnswers} showClues={showClues} />);
}

describe("GuessList", () => {
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
});
