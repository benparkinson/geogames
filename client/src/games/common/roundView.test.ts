import { describe, expect, it } from "vitest";
import { makeRoundProps } from "../../test/fixtures";
import type { Guess, GuessState } from "./guessReducer";
import { liveProgress, roundView } from "./roundView";

const HINTS = { description: "", text: (i: number) => `hint ${i + 1}` };

function playing(overrides: Partial<GuessState> = {}): GuessState {
  return { guesses: [], gaveUp: false, outOfGuesses: false, over: false, hintsUsed: 0, foundAtHint: [], ...overrides };
}

const right = (name: string): Guess => ({ text: name, answer: name });
const wrong = (text: string): Guess => ({ text, answer: null });

describe("roundView", () => {
  it("shows what a round in play is worth now, with no outcome yet", () => {
    const view = roundView(makeRoundProps(), playing({ hintsUsed: 1 }), 1, HINTS);

    expect(view).toMatchObject({ answerState: "UNANSWERED", points: 83, outcome: null, result: null });
  });

  it("calls a win with no hints and no wrong guess a first guess", () => {
    const state = playing({ over: true, guesses: [right("France")] });

    expect(roundView(makeRoundProps(), state, 1, HINTS)).toMatchObject({
      answerState: "CORRECT",
      points: 100,
      outcome: "firstGuess",
      result: "firstGuess",
    });
  });

  it("calls a win after a wrong guess correct", () => {
    const state = playing({ over: true, guesses: [wrong("Spain"), right("France")] });

    expect(roundView(makeRoundProps(), state, 1, HINTS)).toMatchObject({
      outcome: "correct",
      result: "correct",
      wrongGuesses: 1,
    });
  });

  it("scores a win with a hint less, as hints", () => {
    const state = playing({ over: true, hintsUsed: 1, guesses: [right("France")] });

    expect(roundView(makeRoundProps(), state, 1, HINTS)).toMatchObject({ points: 83, result: "hints" });
  });

  it("scores the share found of a round given up, as hints, not missed", () => {
    const state = playing({ over: true, gaveUp: true, guesses: [right("A"), right("B")] });

    expect(roundView(makeRoundProps(), state, 3, HINTS)).toMatchObject({
      answerState: "GAVE_UP",
      points: 67,
      answersFound: 2,
      outcome: "hints",
      result: "revealed",
    });
  });

  it("scores a round given up with nothing found as missed", () => {
    const state = playing({ over: true, gaveUp: true });

    expect(roundView(makeRoundProps(), state, 3, HINTS)).toMatchObject({ points: 0, outcome: "missed" });
  });

  it("tells running out of guesses from giving up", () => {
    const state = playing({ over: true, gaveUp: true, outOfGuesses: true, guesses: [right("A")] });

    expect(roundView(makeRoundProps(), state, 2, HINTS)).toMatchObject({
      answerState: "OUT_OF_GUESSES",
      points: 50,
      result: "outOfGuesses",
    });
  });

  it("lists the text of the hints taken, skipping hints without text", () => {
    const hints = { description: "", text: (i: number) => (i === 0 ? null : `hint ${i + 1}`) };

    expect(roundView(makeRoundProps(), playing({ hintsUsed: 2 }), 1, hints).hintTexts).toEqual(["hint 2"]);
  });

  it("words the continent for one or two", () => {
    expect(roundView(makeRoundProps(), playing(), 1, HINTS).continentText).toBeNull();
    expect(roundView(makeRoundProps({ continents: ["Europe"] }), playing(), 1, HINTS).continentText).toBe(
      "Continent: Europe",
    );
    expect(roundView(makeRoundProps({ continents: ["Europe", "Asia"] }), playing(), 1, HINTS).continentText).toBe(
      "Continents: Europe / Asia",
    );
  });
});

describe("liveProgress", () => {
  it("leaves the server's results alone while the round is in play", () => {
    const round = makeRoundProps({ score: 183 });
    const progress = liveProgress(round, roundView(round, playing(), 1, HINTS));

    expect(progress.score).toBe(183);
    expect(progress.states).toEqual(round.roundAnswerStates);
    expect(progress.outcomes[round.currentRound]).toBeNull();
  });

  it("adds a round just finished here to the score before the server's copy is refetched", () => {
    const round = makeRoundProps({ score: 100 });
    const state = playing({ over: true, hintsUsed: 1, guesses: [right("France")] });
    const progress = liveProgress(round, roundView(round, state, 1, HINTS));

    // A tripoint round (3 hints) answered with one hint is worth 83.
    expect(progress.score).toBe(183);
    expect(progress.points[round.currentRound]).toBe(83);
    expect(progress.outcomes[round.currentRound]).toBe("hints");
  });

  it("does not count an already answered round twice", () => {
    const round = makeRoundProps({ score: 183, answerState: "CORRECT", hintsUsed: 1, points: 83 });
    const state = playing({ over: true, hintsUsed: 1, guesses: [right("France")] });

    expect(liveProgress(round, roundView(round, state, 1, HINTS)).score).toBe(183);
  });

  it("adds nothing for a round revealed here with nothing found", () => {
    const round = makeRoundProps({ score: 100 });
    const state = playing({ over: true, gaveUp: true, hintsUsed: 2 });
    const progress = liveProgress(round, roundView(round, state, 1, HINTS));

    expect(progress.score).toBe(100);
    expect(progress.outcomes[round.currentRound]).toBe("missed");
  });

  it("does not mutate the round it was given", () => {
    const round = makeRoundProps();
    const states = [...round.roundAnswerStates];
    const points = [...round.roundPoints];

    liveProgress(round, roundView(round, playing({ over: true, gaveUp: true }), 1, HINTS));

    expect(round.roundAnswerStates).toEqual(states);
    expect(round.roundPoints).toEqual(points);
  });
});
