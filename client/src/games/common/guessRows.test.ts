import { describe, expect, it } from "vitest";
import { guessListRows } from "./guessRows";
import type { Guess, GuessState } from "./guessReducer";

function stateOf(guesses: Guess[], overrides: Partial<GuessState> = {}): GuessState {
  return { guesses, gaveUp: false, outOfGuesses: false, over: false, hintsUsed: 0, foundAtHint: [], ...overrides };
}

describe("guessListRows", () => {
  it("has no rows before the first guess", () => {
    expect(guessListRows(stateOf([]), ["A", "B", "C"])).toEqual([]);
  });

  it("lists guesses in the order they were made, ", () => {
    const state = stateOf([
      { text: "peru", answer: null },
      { text: "france", answer: "France" },
      { text: "Chile", answer: null, clue: { km: 10, bearing: 90 } },
    ]);

    expect(guessListRows(state, ["France", "Spain", "Italy"])).toEqual([
      { kind: "wrong", text: "peru" },
      { kind: "found", text: "France" },
      { kind: "wrong", text: "Chile", clue: { km: 10, bearing: 90 } },
    ]);
  });

  it("shows the answers that were not found as missed once revealed, whatever order they were found in", () => {
    const state = stateOf([{ text: "italy", answer: "Italy" }], { gaveUp: true, over: true });

    expect(guessListRows(state, ["France", "Spain", "Italy"])).toEqual([
      { kind: "found", text: "Italy" },
      { kind: "missed", text: "France" },
      { kind: "missed", text: "Spain" },
    ]);
  });

  it("lists a wrong guess in a round with one answer", () => {
    expect(guessListRows(stateOf([{ text: "peru", answer: null }]), ["France"])).toEqual([
      { kind: "wrong", text: "peru" },
    ]);
  });
});
