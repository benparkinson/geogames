import { describe, expect, it } from "vitest";
import type { RecordedGuess } from "../../api/types";
import type { AnswerState } from "./Model";
import { GuessConfig, GuessState, guessesLeft, guessReducer, restoreGuessState, wrongGuessCount } from "./guessReducer";

const answers = [
  { name: "Belgium", additionalNames: [] },
  { name: "Germany", additionalNames: ["Deutschland"] },
  { name: "Netherlands", additionalNames: ["Holland"] },
];

const config: GuessConfig = { answers, answerCount: 3, hintsTotal: 2, guessLimit: 99 };
const restored = (answerState: AnswerState, hintsUsed: number, saved: RecordedGuess[] = []) =>
  restoreGuessState(answerState, hintsUsed, saved, [], config).state;
const fresh = () => restored("UNANSWERED", 0);

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

describe("restoreGuessState hints", () => {
  it("restores the hints the server recorded for an answered round", () => {
    expect(restored("CORRECT", 2).hintsUsed).toBe(2);
    expect(restored("GAVE_UP", 1).hintsUsed).toBe(1);
  });

  it("starts an unanswered round with no hints", () => {
    expect(restored("UNANSWERED", 0).hintsUsed).toBe(0);
  });

  it("caps the hint action at hintsTotal", () => {
    let state = fresh();
    for (let i = 0; i < 5; i++) {
      state = guessReducer(state, { type: "hint" }, config).state;
    }

    expect(state.hintsUsed).toBe(config.hintsTotal);
  });
});

describe("restoreGuessState", () => {
  it("starts empty for an unanswered round", () => {
    expect(fresh()).toEqual({
      guesses: [],
      gaveUp: false,
      outOfGuesses: false,
      over: false,
      hintsUsed: 0,
      foundAtHint: [],
    });
  });

  it("starts solved, with every answer found, for a correct round", () => {
    const state = restored("CORRECT", 0);

    expect(state.over).toBe(true);
    expect(state.gaveUp).toBe(false);
    expect(state.guesses.map((g) => g.answer)).toEqual(["Belgium", "Germany", "Netherlands"]);
  });

  it("starts revealed for a round that was given up", () => {
    const state = restored("GAVE_UP", 0);

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

    expect(result).toEqual({ kind: "wrong", guess: "Peru", guessesLeft: 98 });
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
    expect(guessAll(["Peru"], cfg).result).toEqual({ kind: "wrong", guess: "Peru", guessesLeft: 98 });
  });

  it("checks duplicates before unknown", () => {
    const cfg: GuessConfig = { ...config, isKnownGuess: () => false };
    const start: GuessState = { ...fresh(), guesses: [{ text: "Belgium", answer: "Belgium" }] };

    expect(guessAll(["belgium"], cfg, start).result).toEqual({ kind: "duplicate", guess: "belgium" });
  });

  it("ignores guesses once the round is over", () => {
    const over = restored("CORRECT", 0);
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
    const over = restored("CORRECT", 0);
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
    const over = restored("GAVE_UP", 0);

    expect(guessReducer(over, { type: "hint" }, config).state.hintsUsed).toBe(0);
  });
});

describe("guessReducer foundAtHint", () => {
  it("records nothing found for a hint taken before any guess", () => {
    const { state } = guessReducer(fresh(), { type: "hint" }, config);

    expect(state.foundAtHint).toEqual([[]]);
  });

  it("records the answers found when each hint was taken, and not wrong guesses", () => {
    let state = guessAll(["Peru", "Belgium"]).state;
    state = guessReducer(state, { type: "hint" }, config).state;
    state = guessAll(["Holland"], config, state).state;
    state = guessReducer(state, { type: "hint" }, config).state;

    expect(state.foundAtHint).toEqual([["Belgium"], ["Belgium", "Netherlands"]]);
  });

  it("does not record a hint that was refused", () => {
    const over = restored("GAVE_UP", 0);

    expect(guessReducer(over, { type: "hint" }, config).state.foundAtHint).toEqual([]);
  });

  it("restores one empty entry per hint for an answered round", () => {
    expect(restored("CORRECT", 2).foundAtHint).toEqual([[], []]);
    expect(restored("GAVE_UP", 1).foundAtHint).toEqual([[]]);
    expect(restored("UNANSWERED", 0).foundAtHint).toEqual([]);
  });
});

describe("guessReducer guess limit", () => {
  const limited: GuessConfig = { ...config, guessLimit: 3 };

  it("counts down the wrong guesses and carries on", () => {
    const first = guessAll(["Peru"], limited);
    expect(first.result).toEqual({ kind: "wrong", guess: "Peru", guessesLeft: 2 });
    expect(first.finished).toBeUndefined();

    const second = guessAll(["Peru", "Chile"], limited);
    expect(second.result).toEqual({ kind: "wrong", guess: "Chile", guessesLeft: 1 });
    expect(second.state.over).toBe(false);
    expect(guessesLeft(second.state, 3)).toBe(1);
    expect(wrongGuessCount(second.state)).toBe(2);
  });

  it("ends the round on the last wrong guess and keeps the correct guesses", () => {
    const { state, result, finished } = guessAll(["Belgium", "Peru", "Chile", "Cuba"], limited);

    expect(result).toEqual({ kind: "outOfGuesses", guess: "Cuba" });
    expect(finished).toBe("OUT_OF_GUESSES");
    expect(state).toMatchObject({ over: true, gaveUp: true, outOfGuesses: true });
    expect(state.guesses.filter((g) => g.answer !== null)).toEqual([{ text: "Belgium", answer: "Belgium" }]);
    expect(guessesLeft(state, 3)).toBe(0);
  });

  it("doesn't count correct, duplicate, unknown or empty guesses", () => {
    const cfg: GuessConfig = { ...limited, guessLimit: 2, isKnownGuess: (g) => g !== "Atlantis" };
    const { state, result } = guessAll(["Belgium", "Belgium", "Atlantis", "  ", "Peru"], cfg);

    expect(result).toEqual({ kind: "wrong", guess: "Peru", guessesLeft: 1 });
    expect(wrongGuessCount(state)).toBe(1);
    expect(state.over).toBe(false);
  });

  it("never ends the round while guesses are left", () => {
    const texts = Array.from({ length: 50 }, (_, i) => "Nowhere" + i);
    const { state, finished } = guessAll(texts);

    expect(state.over).toBe(false);
    expect(finished).toBeUndefined();
  });

  it("ignores guesses after the round ran out", () => {
    const over = guessAll(["Peru", "Chile", "Cuba"], limited).state;

    expect(guessReducer(over, { type: "guess", text: "Belgium" }, limited).result).toEqual({ kind: "over" });
  });

  it("restores an OUT_OF_GUESSES round as revealed", () => {
    const state = restored("OUT_OF_GUESSES", 1);

    expect(state).toEqual({
      guesses: [],
      gaveUp: true,
      outOfGuesses: true,
      over: true,
      hintsUsed: 1,
      foundAtHint: [[]],
    });
  });

  it("giving up is not running out of guesses", () => {
    const { state, finished } = guessReducer(fresh(), { type: "giveUp" }, limited);

    expect(finished).toBe("GAVE_UP");
    expect(state.outOfGuesses).toBe(false);
  });
});

describe("clues on wrong guesses", () => {
  const clue = { km: 123, bearing: 45 };
  const calls: [string, string[]][] = [];
  const withClues: GuessConfig = {
    ...config,
    guessLimit: 3,
    clueFor: (guess, targets) => {
      calls.push([guess, targets]);
      return guess === "Nowhere" ? null : clue;
    },
  };

  it("stores the clue on a wrong guess and reports it", () => {
    calls.length = 0;
    const outcome = guessAll(["France"], withClues);

    expect(outcome.state.guesses[0].clue).toEqual(clue);
    expect(outcome.result).toMatchObject({ kind: "wrong", guess: "France", clue });
  });

  it("works out the clue against the answers not found yet", () => {
    calls.length = 0;
    guessAll(["Holland", "France"], withClues);

    expect(calls).toEqual([["France", ["Belgium", "Germany"]]]);
  });

  it("gives the guess that runs out the guesses a clue too", () => {
    const outcome = guessAll(["France", "Spain", "Italy"], withClues);

    expect(outcome.result).toMatchObject({ kind: "outOfGuesses", clue });
    expect(outcome.state.guesses[2].clue).toEqual(clue);
  });

  it("stores nothing for correct, duplicate and unknown guesses", () => {
    calls.length = 0;
    const cfg = { ...withClues, isKnownGuess: (g: string) => g !== "Zzz" };
    const outcome = guessAll(["Belgium", "Belgium", "Zzz", ""], cfg);

    expect(calls).toEqual([]);
    expect(outcome.state.guesses).toEqual([{ text: "Belgium", answer: "Belgium" }]);
  });

  it("stores no clue when the guess can't be located", () => {
    const outcome = guessAll(["Nowhere"], withClues);

    expect(outcome.state.guesses[0]).toEqual({ text: "Nowhere", answer: null });
    expect(outcome.result).toMatchObject({ kind: "wrong" });
  });

  it("is unchanged without clueFor", () => {
    const outcome = guessAll(["France"]);

    expect(outcome.state.guesses[0]).toEqual({ text: "France", answer: null });
  });
});

describe("restoreGuessState with saved guesses", () => {
  const saved = (text: string, correct = false): RecordedGuess => ({ text, correct });
  const restore = (
    state: AnswerState,
    guesses: RecordedGuess[],
    cfg: GuessConfig = config,
    hints = 0,
    hintsTakenAt: number[] = [],
  ) => restoreGuessState(state, hints, guesses, hintsTakenAt, cfg);

  it("replays the guesses of an unanswered round", () => {
    const { state, finished } = restore("UNANSWERED", [saved("Peru"), saved("Deutschland", true)]);

    expect(state.guesses).toEqual([
      { text: "Peru", answer: null },
      { text: "Deutschland", answer: "Germany" },
    ]);
    expect(state.over).toBe(false);
    expect(finished).toBeUndefined();
    expect(wrongGuessCount(state)).toBe(1);
  });

  it("works the answer out again instead of trusting the stored flag", () => {
    const { state } = restore("UNANSWERED", [saved("Belgium", false), saved("Peru", true)]);

    expect(state.guesses.map((g) => g.answer)).toEqual(["Belgium", null]);
  });

  it("makes a repeat of a replayed guess a duplicate", () => {
    const { state } = restore("UNANSWERED", [saved("Peru")]);

    expect(guessReducer(state, { type: "guess", text: "peru" }, config).result).toEqual({
      kind: "duplicate",
      guess: "peru",
    });
  });

  it("does not need the list of known names", () => {
    const { state } = restore("UNANSWERED", [saved("Peru")], { ...config, isKnownGuess: () => false });

    expect(state.guesses).toHaveLength(1);
  });

  it("finishes OUT_OF_GUESSES when the replay reaches the limit", () => {
    const limited = { ...config, guessLimit: 2 };

    const { state, finished } = restore("UNANSWERED", [saved("Peru"), saved("Chile")], limited);

    expect(finished).toBe("OUT_OF_GUESSES");
    expect(state.over).toBe(true);
    expect(state.outOfGuesses).toBe(true);
  });

  it("finishes CORRECT when the replay finds every answer", () => {
    const { finished } = restore("UNANSWERED", [
      saved("Belgium", true),
      saved("Germany", true),
      saved("Holland", true),
    ]);

    expect(finished).toBe("CORRECT");
  });

  it("stops replaying once the round is over", () => {
    const limited = { ...config, guessLimit: 1 };

    const { state } = restore("UNANSWERED", [saved("Peru"), saved("Chile")], limited);

    expect(state.guesses).toHaveLength(1);
  });

  it("shows the wrong guesses first in a correct round", () => {
    const { state } = restore("CORRECT", [saved("Peru"), saved("Belgium", true)], config, 1);

    expect(state.guesses.map((g) => g.text)).toEqual(["Peru", "Belgium", "Germany", "Netherlands"]);
    expect(state.over).toBe(true);
    expect(state.hintsUsed).toBe(1);
    expect(state.foundAtHint).toEqual([[]]);
  });

  it("keeps the answers found before giving up as found", () => {
    const { state, finished } = restore("GAVE_UP", [saved("Peru"), saved("Belgium", true)]);

    expect(finished).toBeUndefined();
    expect(state.gaveUp).toBe(true);
    expect(state.outOfGuesses).toBe(false);
    expect(state.over).toBe(true);
    expect(state.guesses.map((g) => g.answer)).toEqual([null, "Belgium"]);
  });

  it("restores an OUT_OF_GUESSES round with its guesses", () => {
    const { state } = restore("OUT_OF_GUESSES", [saved("Peru"), saved("Chile")], { ...config, guessLimit: 2 }, 2);

    expect(state.outOfGuesses).toBe(true);
    expect(state.guesses).toHaveLength(2);
    expect(state.hintsUsed).toBe(2);
  });
});

describe("restoreGuessState with saved hints", () => {
  const saved = (text: string, correct = false): RecordedGuess => ({ text, correct });

  /** Plays the actions the way MapGame does, noting how many guesses each hint came after. */
  function play(actions: ({ guess: string } | "hint")[]) {
    let state = EMPTY;
    const guesses: RecordedGuess[] = [];
    const hintsTakenAt: number[] = [];
    for (const action of actions) {
      if (action === "hint") {
        hintsTakenAt.push(state.guesses.length);
        state = guessReducer(state, { type: "hint" }, config).state;
      } else {
        const outcome = guessReducer(state, { type: "guess", text: action.guess }, config);
        if (outcome.state.guesses.length > state.guesses.length) {
          guesses.push(saved(action.guess, outcome.state.guesses.at(-1)!.answer !== null));
        }
        state = outcome.state;
      }
    }
    return { state, guesses, hintsTakenAt };
  }

  const EMPTY: GuessState = {
    guesses: [],
    gaveUp: false,
    outOfGuesses: false,
    over: false,
    hintsUsed: 0,
    foundAtHint: [],
  };

  it("rebuilds foundAtHint exactly: guess, hint, guess, hint", () => {
    const { state, guesses, hintsTakenAt } = play([
      { guess: "Belgium" },
      "hint",
      { guess: "Peru" },
      { guess: "Germany" },
      "hint",
    ]);
    expect(state.foundAtHint).toEqual([["Belgium"], ["Belgium", "Germany"]]);
    expect(hintsTakenAt).toEqual([1, 3]);

    const restored = restoreGuessState("UNANSWERED", 0, guesses, hintsTakenAt, config).state;

    expect(restored.foundAtHint).toEqual(state.foundAtHint);
    expect(restored.hintsUsed).toBe(2);
    expect(restored.guesses).toEqual(state.guesses);
  });

  it("restores a hint taken before any guess and one taken after the last", () => {
    const { state, guesses, hintsTakenAt } = play(["hint", { guess: "Belgium" }, "hint"]);

    const restored = restoreGuessState("UNANSWERED", 0, guesses, hintsTakenAt, config).state;

    expect(restored.foundAtHint).toEqual([[], ["Belgium"]]);
    expect(restored).toEqual(state);
  });

  it("applies a hint whose count is beyond the last guess at the end", () => {
    const restored = restoreGuessState("UNANSWERED", 0, [saved("Belgium", true)], [7], config).state;

    expect(restored.hintsUsed).toBe(1);
    expect(restored.foundAtHint).toEqual([["Belgium"]]);
  });

  it("caps extra entries at hintsTotal", () => {
    const restored = restoreGuessState("UNANSWERED", 0, [], [0, 0, 0, 0], config).state;

    expect(restored.hintsUsed).toBe(config.hintsTotal);
  });

  it("uses the recorded hint moments for an answered round, and the server's hint count", () => {
    const restored = restoreGuessState(
      "CORRECT",
      2,
      [saved("Belgium", true), saved("Germany", true)],
      [0, 1],
      config,
    ).state;

    expect(restored.hintsUsed).toBe(2);
    expect(restored.foundAtHint).toEqual([[], ["Belgium"]]);
  });

  it("pads an answered round's hints the server counted but has no moment for", () => {
    const restored = restoreGuessState("GAVE_UP", 2, [], [], config).state;

    expect(restored.foundAtHint).toEqual([[], []]);
  });
});
