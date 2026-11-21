import { describe, expect, it } from "vitest";
import { shareText } from "./shareText";

const result = {
  score: 430,
  maxScore: 500,
  rounds: ["firstGuess", "hints", "correct", "missed", "firstGuess"] as const,
};

describe("shareText", () => {
  it("is the title, the score, one square per round and the link", () => {
    expect(
      shareText(
        { date: "2026-11-18", difficulty: "NORMAL", result: { ...result, rounds: [...result.rounds] } },
        "https://example.org",
      ),
    ).toBe("GeoGauntlet Daily 2026-11-18\n430 / 500\n❇️🟨🟩🟥❇️\nhttps://example.org/daily");
  });

  it("marks Easy and Hard mode in the title, and Normal not at all", () => {
    const text = (difficulty: "EASY" | "NORMAL" | "HARD") =>
      shareText(
        { date: "2026-11-18", difficulty, result: { ...result, rounds: [...result.rounds] } },
        "http://localhost:8080",
      );

    expect(text("EASY").split("\n")[0]).toBe("GeoGauntlet Daily 2026-11-18 · Easy");
    expect(text("HARD").split("\n")[0]).toBe("GeoGauntlet Daily 2026-11-18 · Hard mode");
    expect(text("NORMAL").split("\n")[0]).toBe("GeoGauntlet Daily 2026-11-18");
    expect(text("EASY").split("\n").slice(1)).toEqual(text("NORMAL").split("\n").slice(1));
    expect(text("HARD").split("\n").slice(1)).toEqual(text("NORMAL").split("\n").slice(1));
  });

  it("uses the origin it is given and links to /daily, not to a game", () => {
    const text = shareText(
      {
        date: "2026-11-18",
        difficulty: "NORMAL",
        result: { score: 0, maxScore: 500, rounds: ["missed", "missed", "missed", "missed", "missed"] },
      },
      "https://a.b",
    );

    expect(text.endsWith("\nhttps://a.b/daily")).toBe(true);
    expect(text).toContain("🟥🟥🟥🟥🟥");
    expect(text).not.toContain("/games/");
  });
});
