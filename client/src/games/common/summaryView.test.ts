import { describe, expect, it } from "vitest";
import { toRoundView } from "../../api/games";
import { makeGameRounds, RoundSpec } from "../../test/fixtures";
import { summariseGame } from "./summaryView";

function summary(specs: RoundSpec[], difficulty?: "EASY" | "HARD") {
  const rounds = makeGameRounds(specs).map((r) => toRoundView(difficulty ? { ...r, difficulty } : r));
  return summariseGame(rounds);
}

describe("summariseGame", () => {
  it("calls a game of rounds worth 10 points perfect", () => {
    const s = summary([
      { answerState: "CORRECT", firstGuess: true },
      { answerState: "CORRECT", firstGuess: true },
    ]);
    expect(s.heading).toBe("perfect");
    expect(s.perfectRounds).toBe(2);
    expect(s.score).toBe(20);
  });

  it("does not call a game perfect when a wrong guess cost a point", () => {
    const s = summary([{ answerState: "CORRECT" }, { answerState: "CORRECT", wrongGuesses: ["Spain"] }]);
    expect(s.heading).toBe("scored");
    expect(s.perfectRounds).toBe(1);
    expect(s.rows[1].breakdown).toEqual({ answer: 4, noHints: 3, noMisses: 2 });
  });

  it("scores a game with hints or a revealed round", () => {
    const s = summary([{ answerState: "CORRECT", hintsUsed: 1 }, { answerState: "GAVE_UP" }]);
    expect(s.heading).toBe("scored");
    expect(s.rows.map((r) => r.result)).toEqual(["correct", "revealed"]);
    expect(s.rows[0].hintsUsed).toBe(1);
  });

  it("names the type once for one type and not for a mix", () => {
    const one = summary([{ answerState: "CORRECT" }, { answerState: "CORRECT" }]);
    expect(one.mixed).toBe(false);
    expect(one.gameTitle).toBeTruthy();
    const mix = summary([{ answerState: "CORRECT" }, { river: true, answerState: "CORRECT" }]);
    expect(mix.mixed).toBe(true);
    expect(mix.gameTitle).toBeNull();
    expect(mix.playAgainType).not.toBe(one.playAgainType);
  });

  it("returns the game's difficulty", () => {
    const specs: RoundSpec[] = [{ answerState: "CORRECT", firstGuess: true }];
    expect(summary(specs).difficulty).toBe("NORMAL");
    expect(summary(specs, "EASY").difficulty).toBe("EASY");
    expect(summary(specs, "HARD").difficulty).toBe("HARD");
  });

  it("marks an out-of-guesses row", () => {
    const rounds = makeGameRounds([{ answerState: "OUT_OF_GUESSES" }]).map(toRoundView);
    const row = summariseGame(rounds).rows[0];
    expect(row.outOfGuesses).toBe(true);
    expect(row.result).toBe("outOfGuesses");
    expect(row.outcome).toBe("missed");
  });
});

describe("summariseGame totals", () => {
  it("counts perfect rounds and hints", () => {
    const s = summary([
      { answerState: "CORRECT", firstGuess: true },
      { answerState: "CORRECT", hintsUsed: 2, wrongGuesses: ["France", "Spain"] },
      { river: true, answerState: "GAVE_UP", hintsUsed: 1, wrongGuesses: ["Rhine"] },
    ]);
    expect(s.perfectRounds).toBe(1);
    expect(s.hintsUsed).toBe(3);
  });

  it("is zero for a game with no perfect round, no wrong guesses and no hints", () => {
    const s = summary([{ answerState: "GAVE_UP" }]);
    expect([s.perfectRounds, s.hintsUsed]).toEqual([0, 0]);
  });

  it("lists each round's wrong guesses in order, leaving out correct ones", () => {
    const rounds = makeGameRounds([
      { answerState: "CORRECT", wrongGuesses: ["France", "Spain"] },
      { answerState: "CORRECT" },
    ]).map((r, i) => toRoundView(i === 0 ? { ...r, guesses: [...r.guesses, { text: "Belgium", correct: true }] } : r));
    expect(summariseGame(rounds).rows.map((r) => r.wrongGuessNames)).toEqual([["France", "Spain"], []]);
  });

  it("lists the answers a revealed round didn't find, matching a guess by its alias", () => {
    const rounds = makeGameRounds([{ answerState: "GAVE_UP" }, { answerState: "CORRECT" }]).map((r, i) =>
      toRoundView(i === 0 ? { ...r, guesses: [{ text: "Holland", correct: true }] } : r),
    );
    const rows = summariseGame(rounds).rows;
    expect(rows[0].answers).toEqual(["Belgium", "Netherlands", "Germany"]);
    expect(rows[0].missed).toEqual(["Belgium", "Germany"]);
    expect(rows[1].missed).toEqual([]);
  });
});
