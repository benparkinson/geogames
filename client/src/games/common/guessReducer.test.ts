import { describe, expect, it } from "vitest";
import { GuessConfig, GuessState, guessReducer, initialGuessState } from "./guessReducer";

const answers = [
  { name: "Belgium", additionalNames: [] },
  { name: "Germany", additionalNames: ["Deutschland"] },
  { name: "Netherlands", additionalNames: ["Holland"] },
];

const config: GuessConfig = { answers, answerCount: 3, hintsTotal: 2 };
const fresh = () => initialGuessState("UNANSWERED", answers);

/** Applies guesses in order and returns the last outcome. */
function guessAll(texts: string[], cfg: GuessConfig = config, start: GuessState = fresh()) {
  let state = start;
  let outcome = { state } as ReturnType<typeof guessReducer>;
  for (const text of texts) {
    outcome = guessReducer(state, { type: "guess", text }, cfg);
    state = outcome.state;
  }
  return outcome;
}

describe("initialGuessState", () => {
  it("starts empty for an unanswered round", () => {
    expect(fresh()).toEqual({ guesses: [], gaveUp: false, over: false, hintsUsed: 0 });
  });

  it("starts solved, with every answer found, for a correct round", () => {
    const state = initialGuessState("CORRECT", answers);

    expect(state.over).toBe(true);
    expect(state.gaveUp).toBe(false);
    expect(state.guesses.map((g) => g.answer)).toEqual(["Belgium", "Germany", "Netherlands"]);
  });

  it("starts revealed for a round that was given up", () => {
    const state = initialGuessState("GAVE_UP", answers);

    expect(state.over).toBe(true);
    expect(state.gaveUp).toBe(true);
    expect(state.guesses).toEqual([]);
  });
});

describe("guessReducer guesses", () => {
  it("reports empty for blank input", () => {
    expect(guessAll(["   "]).result).toEqual({ kind: "empty" });
  });

  it("records a correct guess under the canonical name and counts down", () => {
    const { state, result, finished } = guessAll(["holland"]);

    expect(result).toEqual({ kind: "correct", remaining: 2 });
    expect(state.guesses).toEqual([{ text: "holland", answer: "Netherlands" }]);
    expect(finished).toBeUndefined();
  });

  it("records a wrong guess without an answer", () => {
    const { state, result } = guessAll(["Peru"]);

    expect(result).toEqual({ kind: "wrong", guess: "Peru" });
    expect(state.guesses).toEqual([{ text: "Peru", answer: null }]);
  });

  it("finishes with CORRECT on the last answer", () => {
    const { state, result, finished } = guessAll(["Belgium", "Germany", "Holland"]);

    expect(result).toEqual({ kind: "correct", remaining: 0 });
    expect(finished).toBe("CORRECT");
    expect(state.over).toBe(true);
  });

  it("treats a repeated guess as a duplicate, whatever its case", () => {
    const { state, result } = guessAll(["Belgium", "BELGIUM"]);

    expect(result).toEqual({ kind: "duplicate", guess: "BELGIUM" });
    expect(state.guesses).toHaveLength(1);
  });

  it("treats a repeated wrong guess as a duplicate", () => {
    expect(guessAll(["Peru", "peru"]).result).toEqual({ kind: "duplicate", guess: "peru" });
  });

  it("treats an alias of a found answer as a duplicate and names the answer", () => {
    const { state, result } = guessAll(["Germany", "Deutschland"]);

    expect(result).toEqual({ kind: "duplicate", guess: "Germany" });
    expect(state.guesses).toHaveLength(1);
  });

  it("treats the name of an answer found by alias as a duplicate", () => {
    const { state, result } = guessAll(["Deutschland", "Germany"]);

    expect(result).toEqual({ kind: "duplicate", guess: "Germany" });
    expect(state.guesses).toEqual([{ text: "Deutschland", answer: "Germany" }]);
  });

  it("reports unknown only for guesses the config rejects", () => {
    const cfg: GuessConfig = { ...config, isKnownGuess: (g) => g !== "Atlantis" };

    expect(guessAll(["Atlantis"], cfg).result).toEqual({ kind: "unknown", guess: "Atlantis" });
    expect(guessAll(["Atlantis"], cfg).state.guesses).toEqual([]);
    expect(guessAll(["Peru"], cfg).result).toEqual({ kind: "wrong", guess: "Peru" });
  });

  it("checks duplicates before unknown", () => {
    const cfg: GuessConfig = { ...config, isKnownGuess: () => false };
    const start: GuessState = { ...fresh(), guesses: [{ text: "Belgium", answer: "Belgium" }] };

    expect(guessAll(["belgium"], cfg, start).result).toEqual({ kind: "duplicate", guess: "belgium" });
  });

  it("ignores guesses once the round is over", () => {
    const over = initialGuessState("CORRECT", answers);
    const outcome = guessReducer(over, { type: "guess", text: "Belgium" }, config);

    expect(outcome.result).toEqual({ kind: "over" });
    expect(outcome.state).toBe(over);
  });
});

describe("guessReducer giving up", () => {
  it("finishes with GAVE_UP and keeps the guesses already made", () => {
    const before = guessAll(["Peru", "Belgium"]).state;
    const { state, finished } = guessReducer(before, { type: "giveUp" }, config);

    expect(finished).toBe("GAVE_UP");
    expect(state.gaveUp).toBe(true);
    expect(state.over).toBe(true);
    expect(state.guesses).toEqual(before.guesses);
  });

  it("does nothing when the round is already over", () => {
    const over = initialGuessState("CORRECT", answers);
    const outcome = guessReducer(over, { type: "giveUp" }, config);

    expect(outcome.state).toBe(over);
    expect(outcome.finished).toBeUndefined();
  });
});

describe("guessReducer hints", () => {
  it("counts hints up to the total", () => {
    let state = fresh();
    for (let i = 0; i < 5; i++) {
      state = guessReducer(state, { type: "hint" }, config).state;
    }

    expect(state.hintsUsed).toBe(2);
  });

  it("gives no hints once the round is over", () => {
    const over = initialGuessState("GAVE_UP", answers);

    expect(guessReducer(over, { type: "hint" }, config).state.hintsUsed).toBe(0);
  });
});
