import { describe, expect, it } from "vitest";
import { challengePassOnText, challengeReplyText, challengeShareText } from "./shareText";

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

const result = { score: 72, maxScore: 100, rounds: ["full" as const, "missed" as const] };

describe("challengeReplyText", () => {
  it("is the score and place, one emoji per round and the challenge link", () => {
    expect(challengeReplyText(result, 2, 6, "abc")).toBe(
      "I took your Geoggernaut challenge: 72 / 100, 2nd of 6.\n🟩🟥\nhttps://geoggernaut.com/challenge?id=abc",
    );
  });

  it.each([
    [1, "1st"],
    [2, "2nd"],
    [3, "3rd"],
    [4, "4th"],
    [11, "11th"],
    [12, "12th"],
    [13, "13th"],
    [21, "21st"],
    [111, "111th"],
  ])("calls place %i %s", (rank, word) => {
    expect(challengeReplyText(result, rank, 200, "abc")).toContain(`, ${word} of 200.`);
  });
});

describe("challengePassOnText", () => {
  it("gives the player's score and the one to beat", () => {
    expect(challengePassOnText(result, 80, false, "abc")).toBe(
      "I scored 72 / 100 on this Geoggernaut challenge. The top score is 80 / 100, can you beat it?\nhttps://geoggernaut.com/challenge?id=abc",
    );
  });

  it("says so when the player tops the table", () => {
    expect(challengePassOnText({ ...result, score: 80 }, 80, true, "abc")).toBe(
      "I top the table with 80 / 100, can you beat it?\nhttps://geoggernaut.com/challenge?id=abc",
    );
  });
});
