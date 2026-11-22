import { describe, expect, it } from "vitest";
import { toRoundView } from "../../api/games";
import { makeGameRounds, RoundSpec } from "../../test/fixtures";
import { summariseGame } from "./summaryView";

function summary(specs: RoundSpec[], difficulty?: "EASY" | "HARD") {
  const rounds = makeGameRounds(specs).map((r) => toRoundView(difficulty ? { ...r, difficulty } : r));
  return summariseGame(rounds);
}

describe("summariseGame", () => {
  it("calls a game of first guesses perfect", () => {
    const s = summary([
      { answerState: "CORRECT", firstGuess: true },
      { answerState: "CORRECT", firstGuess: true },
    ]);
    expect(s.heading).toBe("perfect");
    expect(s.score).toBe(200);
    expect(s.summaryLine).toBe("2 first guesses");
  });

  it("calls full marks after wrong guesses full marks, not perfect", () => {
    const s = summary([{ answerState: "CORRECT", firstGuess: true }, { answerState: "CORRECT" }]);
    expect(s.heading).toBe("fullMarks");
    expect(s.summaryLine).toBe("1 first guess · 1 correct");
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

  it("adds the difficulty to the summary line, nothing for Normal", () => {
    const specs: RoundSpec[] = [{ answerState: "CORRECT", firstGuess: true }];
    expect(summary(specs).summaryLine).toBe("1 first guess");
    expect(summary(specs, "EASY").summaryLine).toBe("1 first guess · Easy");
    expect(summary(specs, "HARD").difficulty).toBe("HARD");
    expect(summary(specs, "HARD").summaryLine).toBe("1 first guess · Hard mode");
  });

  it("marks an out-of-guesses row", () => {
    const rounds = makeGameRounds([{ answerState: "OUT_OF_GUESSES" }]).map(toRoundView);
    const row = summariseGame(rounds).rows[0];
    expect(row.outOfGuesses).toBe(true);
    expect(row.result).toBe("outOfGuesses");
    expect(row.outcome).toBe("missed");
  });
});
