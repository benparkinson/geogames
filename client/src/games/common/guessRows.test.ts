import { describe, expect, it } from "vitest";
import { guessListRows } from "./guessRows";
import type { Guess, GuessState } from "./guessReducer";

function stateOf(guesses: Guess[], overrides: Partial<GuessState> = {}): GuessState {
  return { guesses, gaveUp: false, outOfGuesses: false, over: false, hintsUsed: 0, foundAtHint: [], ...overrides };
}

describe("guessListRows", () => {
  it("has a placeholder for every answer before the first guess", () => {
    expect(guessListRows(stateOf([]), 3, ["A", "B", "C"])).toEqual([
      { kind: "empty" },
      { kind: "empty" },
      { kind: "empty" },
    ]);
  });

  it("lists guesses in the order they were made, then the placeholders", () => {
    const state = stateOf([
      { text: "peru", answer: null },
      { text: "france", answer: "France" },
      { text: "Chile", answer: null, clue: { km: 10, bearing: 90 } },
    ]);

    expect(guessListRows(state, 3, ["France", "Spain", "Italy"])).toEqual([
      { kind: "wrong", text: "peru" },
      { kind: "found", text: "France" },
      { kind: "wrong", text: "Chile", clue: { km: 10, bearing: 90 } },
      { kind: "empty" },
      { kind: "empty" },
    ]);
  });

  it("shows the answers that were not found as missed once revealed, whatever order they were found in", () => {
    const state = stateOf([{ text: "italy", answer: "Italy" }], { gaveUp: true, over: true });

    expect(guessListRows(state, 3, ["France", "Spain", "Italy"])).toEqual([
      { kind: "found", text: "Italy" },
      { kind: "missed", text: "France" },
      { kind: "missed", text: "Spain" },
    ]);
  });

  it("keeps a placeholder for a revealed answer that has no known name", () => {
    const state = stateOf([], { gaveUp: true, over: true });

    expect(guessListRows(state, 2, ["Spain"])).toEqual([{ kind: "missed", text: "Spain" }, { kind: "empty" }]);
  });

  it("shows nothing as missed before the round is revealed", () => {
    expect(guessListRows(stateOf([]), 2, ["France", "Spain"])).toEqual([{ kind: "empty" }, { kind: "empty" }]);
  });

  it("has no placeholder for a round with one answer", () => {
    expect(guessListRows(stateOf([]), 1, ["France"])).toEqual([]);
    expect(guessListRows(stateOf([{ text: "peru", answer: null }]), 1, ["France"])).toEqual([
      { kind: "wrong", text: "peru" },
    ]);
  });
});
