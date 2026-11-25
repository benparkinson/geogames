import { describe, expect, it } from "vitest";
import { challengeShareText } from "./shareText";

describe("challengeShareText", () => {
  it("is the invitation, the score, one emoji per round and the challenge link", () => {
    const text = challengeShareText(
      { score: 430, maxScore: 500, rounds: ["firstGuess", "hints", "correct", "missed", "firstGuess"] },
      "abc-123",
    );
    expect(text).toBe(
      "I just played this round of Geoggernaut, can you beat my score?\n430 / 500\n❇️🟨🟩🟥❇️\nhttps://geoggernaut.com/challenge?id=abc-123",
    );
  });

  it("links to the challenge, never to a game", () => {
    const text = challengeShareText({ score: 0, maxScore: 100, rounds: ["missed"] }, "id1");
    expect(text).toContain("/challenge?id=id1");
    expect(text).not.toContain("/games/");
  });
});
