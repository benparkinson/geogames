import { describe, expect, it } from "vitest";
import { challengeShareText } from "./shareText";

describe("challengeShareText", () => {
  it("is the invitation, the score, one emoji per round and the challenge link", () => {
    const text = challengeShareText(
      { score: 43, maxScore: 50, rounds: ["full", "good", "full", "missed", "partial"] },
      "abc-123",
    );
    expect(text).toBe(
      "I just played this round of Geoggernaut, can you beat my score?\n43 / 50\n🟩🟨🟩🟥🟧\nhttps://geoggernaut.com/challenge?id=abc-123",
    );
  });

  it("links to the challenge, never to a game", () => {
    const text = challengeShareText({ score: 0, maxScore: 10, rounds: ["missed"] }, "id1");
    expect(text).toContain("/challenge?id=id1");
    expect(text).not.toContain("/games/");
  });
});
